package com.project.payment.exception;

import com.project.common.BusinessException;

public class PaymentAmountException extends BusinessException {
	public PaymentAmountException() {
		super("주문 수량과 일치하지 않습니다.");
	}
}