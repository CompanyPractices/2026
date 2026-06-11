package com.processing.e2e.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.processing.e2e.E2EBaseTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.sql.SQLException;

import static org.testng.Assert.assertTrue;

/**
 * TC-07 — Decline: Insufficient Funds (responseCode 51).
 * Goal: check reject based on insufficient funds.
 * Available balance in database must not change
 */
public class InsufficientFundsTest extends E2EBaseTest {

}