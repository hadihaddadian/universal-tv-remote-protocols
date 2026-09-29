package com.haddadian.universalremote.reliability

import org.junit.Test
import org.junit.Assert.assertEquals

class NetworkMonitorContractTest {
    @Test fun recoveryAttemptsAreBounded() { assertEquals(3, RetryPolicy().maxAttempts) }
}
