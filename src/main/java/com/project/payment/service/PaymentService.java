package com.project.payment.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.project.common.config.TossPaymentConfig;
import com.project.member.domain.Member;
import com.project.member.repository.MemberRepository;
import com.project.payment.domain.Payment;
import com.project.payment.dto.PaymentRequestDto;
import com.project.payment.dto.PaymentResponseDto;
import com.project.payment.exception.OrderNotFoundException;
import com.project.payment.exception.PaymentAmountException;
import com.project.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
	private final MemberRepository memberRepository;
	private final PaymentRepository paymentRepository;
	private final TossPaymentConfig tossPaymentConfig;

	//토스 결제 요청
	public PaymentResponseDto requestTossPayment(Long memberId, PaymentRequestDto paymentRequestDto) {
		Member member = memberRepository.getById(memberId);
		Payment payment = Payment.of(member, paymentRequestDto.getPaymentType(), paymentRequestDto.getAmount(),
			paymentRequestDto.getOrderId(), paymentRequestDto.getOrderName(), paymentRequestDto.getSuccessUrl(),
			paymentRequestDto.getFailUrl(), false);
		payment.setMember(member);
		paymentRepository.save(payment);

		String successUrl = paymentRequestDto.getSuccessUrl() == null ? tossPaymentConfig.getSuccessfulUrl() :
			paymentRequestDto.getSuccessUrl();
		String failUrl = paymentRequestDto.getFailUrl() == null ? tossPaymentConfig.getFailUrl() :
			paymentRequestDto.getFailUrl();

		PaymentResponseDto paymentResponseDto = PaymentResponseDto.of(payment, successUrl, failUrl);
		return paymentResponseDto;
	}

	//토스 결제 성공
	public String tossPaymentSuccess(String paymentKey, String orderId, Long amount) throws JSONException {
		Payment payment = verifyPayment(orderId, amount);
		String result = requestPaymentAccept(paymentKey, orderId, amount);

		payment.setPaymentKey(paymentKey); // 결제 성공시 redirect로 받는 paymentKey 저장
		payment.setPaySuccessYN(true); // 결제 성공 -> PaySuccess값 true로 설정
		paymentRepository.save(payment);

		return result;
	}

	// 결제 요청이 유효한지 체크
	public Payment verifyPayment(String orderId, Long amount) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(OrderNotFoundException::new);
		//주문 수량과 결제할 때 주문 수량이 일치하는지 확인
		if (!payment.getAmount().equals(amount)) {
			throw new PaymentAmountException();
		}
		return payment;
	}

	public String requestPaymentAccept(String paymentKey, String orderId, Long amount) throws JSONException {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = getHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject params = new JSONObject();
		params.put("orderId", orderId);
		params.put("amount", amount);

		String requestJson = params.toString();

		HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

		//토스 url로 success response 응답 받음
		ResponseEntity<String> response = restTemplate.exchange(
			TossPaymentConfig.baseUrl + paymentKey,
			HttpMethod.POST,
			entity,
			String.class
		);

		return response.getBody();
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		//tossSecretKey Base64 인코딩, {시크릿key+":"}로 인코딩
		String encodedAuthKey = new String(
			Base64.getEncoder()
				.encode((tossPaymentConfig.getTestSecretApiKey() + ":").getBytes(StandardCharsets.UTF_8)));
		headers.setBasicAuth(encodedAuthKey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return headers;
	}
}


