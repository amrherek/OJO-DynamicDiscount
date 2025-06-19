package com.atos.dynamicdiscount.processor.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.entity.DynDiscConf;
import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonth;
import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonthId;
import com.atos.dynamicdiscount.model.entity.DynDiscOffer;
import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroup;
import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroupId;
import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonth;
import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonthId;
import com.atos.dynamicdiscount.model.entity.DynDiscType;
import com.atos.dynamicdiscount.repository.DynDiscConfRepository;
import com.atos.dynamicdiscount.repository.DynDiscFreeMonthRepository;
import com.atos.dynamicdiscount.repository.DynDiscOfferRepository;
import com.atos.dynamicdiscount.repository.DynDiscPriceGroupRepository;
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

    // Repositories for accessing dynamic discount config
	private final DynDiscTypeRepository dynDiscTypeRepository;
	private final DynDiscConfRepository dynDiscConfRepository;
	private final DynDiscPriceGroupRepository dynDiscPriceGroupRepository;
	private final DynDiscOfferRepository dynDiscOfferRepository;
	private final DynDiscSpecialMonthRepository dynDiscSpecialMonthRepository;
	private final DynDiscFreeMonthRepository dynDiscFreeMonthRepository;
	
	
	// Maps for storing preloaded configuration data
    private Map<Integer, DynDiscType> dynDiscTypeMap; // Maps type ID to DynDiscType
    private Map<Integer, DynDiscConf> dynDiscConfMap; // Maps discount ID to DynDiscConf
    private Map<Integer, List<DynDiscOffer>> dynDiscOfferMap; // Maps discount ID to a list of DynDiscOffers
    private Map<DynDiscPriceGroupId, DynDiscPriceGroup> DynDiscPriceGroupMap; // Maps price group ID to DynDiscPriceGroup
    private Map<DynDiscSpecialMonthId, DynDiscSpecialMonth> dynDiscSpecialMonthMap; // Maps special month ID to DynDiscSpecialMonth
    private Map<DynDiscFreeMonthId, DynDiscFreeMonth> dynDiscFreeMonthMap; // Maps free month ID to DynDiscFreeMonth
	
	

	@PostConstruct
	public void loadConfigurations() {
		log.info("Loading configuration data...");
		dynDiscOfferMap =buildDiscIdToOffersMap();
		dynDiscConfMap = mapEntities(dynDiscConfRepository.findAll(), DynDiscConf::getDiscId);
		dynDiscTypeMap = mapEntities(dynDiscTypeRepository.findAll(), DynDiscType::getTypeId);
		DynDiscPriceGroupMap = mapEntities(dynDiscPriceGroupRepository.findAll(), DynDiscPriceGroup::getId);
		dynDiscSpecialMonthMap = mapEntities(dynDiscSpecialMonthRepository.findAll(), DynDiscSpecialMonth::getId);
		dynDiscFreeMonthMap = mapEntities(dynDiscFreeMonthRepository.findAll(), DynDiscFreeMonth::getId);
		log.info("Configuration data loaded successfully.");
	}

	private <T, K> Map<K, T> mapEntities(Iterable<T> entities, Function<T, K> keyMapper) {
		return ((List<T>) entities).stream()
					.peek(entity -> log.debug("Mapping entity: {}", entity))    // Log each entity being mapped
					.collect(Collectors.toMap(keyMapper, Function.identity())); // Convert the list to a map
	}
	
	
	//Builds a map that groups DynDiscOffer entities by their discount ID (discId).
	public Map<Integer, List<DynDiscOffer>> buildDiscIdToOffersMap() {
		
        // Fetch all offers from the repository
	    List<DynDiscOffer> offers = dynDiscOfferRepository.findAll(); 
	    
        // Group the offers by discId while filtering out those without a discId
	    return offers.stream()
	            .filter(offer -> offer.getDiscId() != null) // Filter out any offers that don't have a discId
	            .collect(Collectors.groupingBy(DynDiscOffer::getDiscId)); // Group by discId
	}

	
	
    // Synchronizes and reloads configuration data into memory.
	public synchronized void refreshConfigurations() {
		log.debug("Refreshing configuration data...");
		loadConfigurations();
	}
}
