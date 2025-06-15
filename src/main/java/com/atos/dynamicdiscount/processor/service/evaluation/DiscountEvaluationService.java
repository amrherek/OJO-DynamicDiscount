package com.atos.dynamicdiscount.processor.service.evaluation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.dto.DynDiscGrantEvalDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscConf;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;
import com.atos.dynamicdiscount.processor.config.DynDiscConfigurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountEvaluationService {
	
	@Value("${spring.datasource.username:DYN_DISC}")
	private String username;
	private final DiscountValidator discountValidator;
	private final DiscountCalculator discountCalculator;
	private final DynDiscConfigurations cfg;

	public DynDiscGrantEvalDTO evaluateDiscounts(DynDiscContract contract, List<DynDiscAssignDTO> discounts,
			LocalDateTime cutoffDate) {

		try {
			if (discounts == null || discounts.isEmpty()) {
				return buildResult(contract, null, null, "S", "No discounts provided");
			}
			
			
			Map<Long, String> discountErrors = new HashMap<>();


			// 1) Partition via validationService
			Map<Boolean, List<DynDiscAssignDTO>> parts = discounts.stream()
				    .collect(Collectors.partitioningBy(d -> {
				        StringBuilder error = new StringBuilder();
				        boolean isValid = discountValidator.isValid(d, cutoffDate, error);
				        if (!isValid) {
				            discountErrors.put(d.getAssignId().longValue(), error.toString());
				        }
				        return isValid;
				    }));
			
			
			// 3) Split into unmodifiable valid/invalid lists
			List<DynDiscAssignDTO> valid = Collections.unmodifiableList(parts.get(true));
			List<DynDiscAssignDTO> invalid = Collections.unmodifiableList(parts.get(false));
			

			// 4) If there are no valid discounts, short-circuit
			if (valid.isEmpty()) {
			    // Rebuild the remark from the discountErrors map
			    String remark = "No valid discount (" + discountErrors.size() + " invalid)";				

			    // Append detailed errors from discountErrors map			    
			    String detailedErrors = discountErrors.entrySet().stream()
			    	    .map(entry -> String.format("[%s]", entry.getValue())) // Wrap each value in brackets
			    	    .collect(Collectors.joining("; "));
			    
		        remark += ": " + detailedErrors;

			    return buildResult(contract, null, null, "S", remark);
			}

			// 3) Otherwise pick the latest valid assignment
			DynDiscAssignDTO latest = valid.stream()
			    .max(Comparator.comparing(DynDiscAssignDTO::getAssignDate)
			         .thenComparing(DynDiscAssignDTO::getAssignId)).get();  

			log.info("✓ AssignId {}: Selected for processing.", latest.getAssignId());


			// 4) Compute grant history via computationService
			DynDiscGrantHistory grant = discountCalculator.compute(contract, latest);

			// 5) Build evaluation history via helper
			DynDiscEvalHistory eval = buildEvalHistory(latest, grant, cutoffDate);
			return buildResult(contract, eval, grant, null,null);

		} catch (Exception ex) {
			String msg = "Error during discount evaluation for contract " + contract.getCoId();
			log.error("✗ {}, {}", msg, ex);
			return buildResult(contract, null, null, "F", msg);
		}
	}

	private DynDiscGrantEvalDTO buildResult(DynDiscContract contract, DynDiscEvalHistory eval,
			DynDiscGrantHistory grant, String status, String remark) {
		contract.setStatus(status);
		contract.setRemark(remark);
		return new DynDiscGrantEvalDTO(contract, eval, grant);
	}

	private DynDiscEvalHistory buildEvalHistory(DynDiscAssignDTO latest, DynDiscGrantHistory grant,
			LocalDateTime cutoffDate) {
		DynDiscConf conf = cfg.getDynDiscConfMap().get(latest.getDiscId().intValue());

		return DynDiscEvalHistory.builder().requestId(grant.getRequestId()).assignId(grant.getAssignId())
				.customerId(latest.getCustomerId().intValue()).coId(latest.getCoId().intValue())
				.billPeriodEndDate(cutoffDate)
				.lbcDate(latest.getLbcDate() != null 
		         ? latest.getLbcDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
		         : null)
				.prgCode(latest.getPrgcode()).tmCode(latest.getTmCode()).discSncode(latest.getDiscSncode().intValue())
				.discId(latest.getDiscId().intValue()).occSncode(conf.getOccSncode()).occGlcode(conf.getOccGlcode())
				.offerSncode(latest.getOfferSnCode().intValue()).occRemark(conf.getOccRemark())
				.offerValidFrom(
						latest.getOfferValidFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
				.offerStatus(String.valueOf(latest.getOfferStatus())).offerPrice(latest.getOfferPrice().floatValue())
				.aloSncode(latest.getAloSnCode().intValue())
				.aloValidFrom(latest.getAloValidFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
				.aloStatus(String.valueOf(latest.getAloStatus())).aloPrice(latest.getAloPrice().floatValue()).username(username).build();
	}

}
