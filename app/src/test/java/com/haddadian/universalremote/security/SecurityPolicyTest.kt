package com.haddadian.universalremote.security
import org.junit.Assert.*
import org.junit.Test
class SecurityPolicyTest{
 @Test fun rejectsHttp(){assertFalse(NetworkSecurityPolicy.isAllowedEndpoint("http://example.com"))}
 @Test fun acceptsHttps(){assertTrue(NetworkSecurityPolicy.isAllowedEndpoint("https://example.com"))}
}