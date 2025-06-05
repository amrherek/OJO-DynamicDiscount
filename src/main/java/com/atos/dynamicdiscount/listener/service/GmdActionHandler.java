package com.atos.dynamicdiscount.listener.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.atos.dynamicdiscount.enums.DynDiscountState;
import com.atos.dynamicdiscount.enums.GmdAction;
import com.atos.dynamicdiscount.model.dto.GmdRequestHistoryDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscAssign;
import com.atos.dynamicdiscount.model.entity.DynDiscAssignState;
import com.atos.dynamicdiscount.repository.DynDiscAssignRepository;
import com.atos.dynamicdiscount.repository.DynDiscAssignStateRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GmdActionHandler {

	@Value("${spring.datasource.username:DYN_DISC}")
	private String username;
	
	@Autowired
	private DynDiscAssignRepository dynDiscAssignRepository;

	@Autowired
	private DynDiscAssignStateRepository dynDiscAssignStateRepository;

	public void handleAction(GmdRequestHistoryDTO request) {
		int actionId = request.getActionId().intValue();
		GmdAction action = getActionById(actionId);

		switch (action) {
		case ACTIVATE_CONTRACT, ASSIGN_SERVICE -> addNewDiscountInstance(request);
		case DEACTIVATE_CONTRACT, DEACTIVATE_SUSPENED_CONTRACT, REMOVE_SERVICE ->
			updateDiscountInstanceStatus(request, DynDiscountState.DEACTIVE.getCode());
		case REACTIVATE_CONTRACT -> updateDiscountInstanceStatus(request, DynDiscountState.ACTIVE.getCode());
		case SUSPENDE_CONTRACT -> updateDiscountInstanceStatus(request, DynDiscountState.SUSPENDED.getCode());
		default -> log.warn("Unhandled action ID: {} for request ID {}", actionId, request.getRequest());
		}
	}

	private GmdAction getActionById(int actionId) {
		for (GmdAction action : GmdAction.values()) {
			if (action.getAction().equals(actionId)) {
				return action;
			}
		}
		return null;
	}

	@Transactional
	public void addNewDiscountInstance(GmdRequestHistoryDTO request) {
		try {
			DynDiscAssign assign = buildNewDiscountAssign(request);
			assign = dynDiscAssignRepository.save(assign);
			addAssignState(assign.getAssignId(), request, DynDiscountState.ACTIVE.getCode());
			logActionSuccess(request, DynDiscountState.ACTIVE.getCode(), assign.getAssignId(), true);
		} catch (DataIntegrityViolationException e) {
			e.printStackTrace();
			handleDataIntegrityViolation(e, request);
		}
	}

	@Transactional
	private void updateDiscountInstanceStatus(GmdRequestHistoryDTO request, String status) {
		Optional<DynDiscAssign> dynDiscAssign = dynDiscAssignRepository.findLatestAssign(request.getCoId().intValue(),
				request.getDiscSncode());
		if (dynDiscAssign.isPresent()) {
			try {
				DynDiscAssign assign = dynDiscAssign.get();
				if (DynDiscountState.DEACTIVE.getCode().equals(status)) {
					assign.setDeleteDate(
							request.getValidFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
					dynDiscAssignRepository.save(assign);
				}
				addAssignState(assign.getAssignId(), request, status);
				logActionSuccess(request, status, assign.getAssignId(), false); // Status updated
			} catch (DataIntegrityViolationException e) {
				e.printStackTrace();
				handleDataIntegrityViolation(e, request);
			}
		} else {
			log.error("  GMD_REQUEST={} with ACTION_ID={}, No discount instance found. CO_ID={}, DISCOUNT_SNCODE={}.",
					request.getRequest(), request.getActionId(), request.getCoId(), request.getDiscSncode());
		}

	}

	private void addAssignState(Long assignId, GmdRequestHistoryDTO request, String status) {
		Integer maxSeqno = dynDiscAssignStateRepository.findMaxSeqnoByAssignId(assignId);
		DynDiscAssignState assignState = new DynDiscAssignState();
		assignState.setAssignId(assignId);
		assignState.setSeqno(maxSeqno + 1);
		assignState.setActionId(request.getActionId().intValue());
		assignState.setStatus(status);
		assignState
				.setStatusDate(request.getValidFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		assignState.setGmdRequest(request.getRequest().intValue());
		assignState.setEntryDate(LocalDateTime.now());
		dynDiscAssignStateRepository.save(assignState);
	}

	private void handleDataIntegrityViolation(DataIntegrityViolationException e, GmdRequestHistoryDTO request) {
		String errorMessage = e.getCause() instanceof ConstraintViolationException
				? "  Request skipped, entry already exists in DYN_DISC_ASSIGN or DYN_DISC_ASSIGN_STATE table."
				: "Data integrity violation: " + e.getMessage();
		log.error("  GMD_REQUEST={} with ACTION_ID={}, {}", request.getRequest(), request.getActionId(), errorMessage);
		log.debug("  Detailed exception for GMD_REQUEST={}: {}", request, e.getMessage());
	}

	private DynDiscAssign buildNewDiscountAssign(GmdRequestHistoryDTO request) {
		DynDiscAssign assign = new DynDiscAssign();
		assign.setCoId(request.getCoId().intValue());
		assign.setCustomerId(request.getCustomerId().intValue());
		assign.setDiscId(request.getDiscId().intValue());
		assign.setDiscSncode(request.getDiscSncode().intValue());
		assign.setEntryDate(LocalDateTime.now());
		assign.setAssignDate(request.getValidFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		assign.setGmdRequest(request.getRequest().intValue());
		assign.setApplyCount(0);
		assign.setOvwApplyCount(null);
		assign.setUsername(username);
		return assign;
	}

	private void logActionSuccess(GmdRequestHistoryDTO request, String status, Long assignSeq, boolean isNewInstance) {
		if (isNewInstance) {
			log.info("  GMD_REQUEST={} with ACTION_ID={}, New discount instance added. Status: {}, Assign Seq: {}",
					request.getRequest(), request.getActionId(), status, assignSeq);
		} else {
			log.info("  GMD_REQUEST={} with ACTION_ID={}, Discount instance status updated. Status: {}, Assign Seq: {}",
					request.getRequest(), request.getActionId(), status, assignSeq);
		}
	}

}
