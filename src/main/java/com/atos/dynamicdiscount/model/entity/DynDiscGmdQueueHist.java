package com.atos.dynamicdiscount.model.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Historical archive of processed GMD queue entries.
 */
@Getter
@Setter
@Entity
@Table(name = "DYN_DISC_GMD_QUEUE_HIST")
public class DynDiscGmdQueueHist {

	@Id
    @Column(name = "REQUEST")
    private Long request;

    @Column(name = "INSERTED_AT")
    private Date insertedAt;

    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "CO_ID")
    private Long coId;

    @Column(name = "ACTION_ID")
    private Long actionId;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "MOVED_AT")
    private Date movedAt;
}
