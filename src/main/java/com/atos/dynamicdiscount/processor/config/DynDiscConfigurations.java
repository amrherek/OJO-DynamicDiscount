package com.atos.dynamicdiscount.processor.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.entity.DynDiscConf;
import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroup;
import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroupId;
import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonth;
import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonthId;
import com.atos.dynamicdiscount.model.entity.DynDiscOffer;
import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonth;
import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonthId;
import com.atos.dynamicdiscount.model.entity.DynDiscType;
import com.atos.dynamicdiscount.repository.DynDiscConfRepository;
import com.atos.dynamicdiscount.repository.DynDiscPriceGroupRepository;
import com.atos.dynamicdiscount.repository.DynDiscFreeMonthRepository;
import com.atos.dynamicdiscount.repository.DynDiscOfferRepository;
import com.atos.dynamicdiscount.repository.DynDiscSpecialMonthRepository;
import com.atos.dynamicdiscount.repository.DynDiscTypeRepository;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
public class DynDiscConfigurations {

	private final DynDiscTypeRepository dynDiscTypeRepository;
	private final DynDiscConfRepository dynDiscConfRepository;
	private final DynDiscPriceGroupRepository dynDiscPriceGroupRepository;
	private final DynDiscOfferRepository dynDiscOfferRepository;
	private final DynDiscSpecialMonthRepository dynDiscSpecialMonthRepository;
	private final DynDiscFreeMonthRepository dynDiscFreeMonthRepository;

	private Map<Integer, DynDiscType> dynDiscTypeMap;
	private Map<Integer, DynDiscConf> dynDiscConfMap;
	//private Map<Integer, DynDiscOffer> dynDiscOfferMap;
	public Map<Integer, List<DynDiscOffer>> dynDiscOfferMap; 
	private Map<DynDiscPriceGroupId, DynDiscPriceGroup> DynDiscPriceGroupMap;
	private Map<DynDiscSpecialMonthId, DynDiscSpecialMonth> dynDiscSpecialMonthMap;
	private Map<DynDiscFreeMonthId, DynDiscFreeMonth> dynDiscFreeMonthMap;

	@PostConstruct
	public void loadConfigurations() {
		log.info("Loading configuration data...");
		//dynDiscOfferMap = mapEntities(dynDiscOfferRepository.findAll(), DynDiscOffer::getOfferId);
		dynDiscOfferMap =buildDiscIdToOffersMap();
		dynDiscConfMap = mapEntities(dynDiscConfRepository.findAll(), DynDiscConf::getDiscId);
		dynDiscTypeMap = mapEntities(dynDiscTypeRepository.findAll(), DynDiscType::getTypeId);
		DynDiscPriceGroupMap = mapEntities(dynDiscPriceGroupRepository.findAll(), DynDiscPriceGroup::getId);
		dynDiscSpecialMonthMap = mapEntities(dynDiscSpecialMonthRepository.findAll(), DynDiscSpecialMonth::getId);
		dynDiscFreeMonthMap = mapEntities(dynDiscFreeMonthRepository.findAll(), DynDiscFreeMonth::getId);
		log.info("Configuration data loaded successfully.");
	}

	private <T, K> Map<K, T> mapEntities(Iterable<T> entities, Function<T, K> keyMapper) {
		return ((List<T>) entities).stream().peek(entity -> log.debug("Mapping entity: {}", entity))
				.collect(Collectors.toMap(keyMapper, Function.identity()));
	}
	
	
	public Map<Integer, List<DynDiscOffer>> buildDiscIdToOffersMap() {
	    List<DynDiscOffer> offers = dynDiscOfferRepository.findAll(); // Get all offers from the repository
	    
	    // Group offers by discId
	    return offers.stream()
	            .filter(offer -> offer.getDiscId() != null) // Filter out any offers that don't have a discId
	            .collect(Collectors.groupingBy(DynDiscOffer::getDiscId)); // Group by discId
	}

	public synchronized void refreshConfigurations() {
		log.debug("Refreshing configuration data...");
		loadConfigurations();
	}
}
