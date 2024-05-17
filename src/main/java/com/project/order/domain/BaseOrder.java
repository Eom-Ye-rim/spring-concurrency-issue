package com.project.order.domain;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class BaseOrder {

	private String deliveryAddress;

	private boolean defaultAddress;

	private boolean defaultPayMethod;

	private CardCompany cardCompany;

	private String cardNum;
}
