package com.atos.dynamicdiscount.model.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
/*
@SqlResultSetMapping(
	    name = "GmdRequestHistoryMapping",
	    classes = @ConstructorResult(
	        targetClass = GmdRequestHistoryDTO.class,
	        columns = {
	            @ColumnResult(name = "request", type = Long.class),
	            @ColumnResult(name = "customerId", type = Long.class),
	            @ColumnResult(name = "coId", type = Long.class),
	            @ColumnResult(name = "actionId", type = Long.class),
	            @ColumnResult(name = "discId", type = Long.class),
	            @ColumnResult(name = "discSncode", type = Long.class),
	            @ColumnResult(name = "validFromDate", type = LocalDateTime.class)
	        }
	    )
	)
	*/
@Table(name = "GMD_REQUEST_HISTORY")
@Data 
@NoArgsConstructor 
@ToString
public class GmdRequestHistory {

    @Id
    @Column(name = "REQUEST")
    private Long request;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "CO_ID")
    private Long coId;

    @Column(name = "ACTION_ID", nullable = false)
    private Integer actionId;
}



