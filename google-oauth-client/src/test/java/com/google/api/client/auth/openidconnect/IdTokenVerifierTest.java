/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.auth.openidconnect;

import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.auth.openidconnect.IdTokenVerifier.VerificationException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature.Header;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.client.util.Clock;
import com.google.api.client.util.Lists;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Tests {@link IdTokenVerifier}.
 *
 * @author Yaniv Inbar
 */
public class IdTokenVerifierTest extends TestCase {

  private static final String CLIENT_ID = "myclientid";
  private static final String CLIENT_ID2 = CLIENT_ID + "2";

  private static final List<String> TRUSTED_CLIENT_IDS = Arrays.asList(CLIENT_ID, CLIENT_ID2);

  private static final String ISSUER = "issuer.example.com";
  private static final String ISSUER2 = ISSUER + "2";
  private static final String ISSUER3 = ISSUER + "3";

  private static final String ES256_TOKEN =
      "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Im1wZjBEQSJ9.eyJhdWQiOiIvcHJvamVjdHMvNjUyNTYyNzc2Nzk4L2FwcHMvY2xvdWQtc2FtcGxlcy10ZXN0cy1waHAtaWFwIiwiZW1haWwiOiJjaGluZ29yQGdvb2dsZS5jb20iLCJleHAiOjE1ODQwNDc2MTcsImdvb2dsZSI6eyJhY2Nlc3NfbGV2ZWxzIjpbImFjY2Vzc1BvbGljaWVzLzUxODU1MTI4MDkyNC9hY2Nlc3NMZXZlbHMvcmVjZW50U2VjdXJlQ29ubmVjdERhdGEiLCJhY2Nlc3NQb2xpY2llcy81MTg1NTEyODA5MjQvYWNjZXNzTGV2ZWxzL3Rlc3ROb09wIiwiYWNjZXNzUG9saWNpZXMvNTE4NTUxMjgwOTI0L2FjY2Vzc0xldmVscy9ldmFwb3JhdGlvblFhRGF0YUZ1bGx5VHJ1c3RlZCJdfSwiaGQiOiJnb29nbGUuY29tIiwiaWF0IjoxNTg0MDQ3MDE3LCJpc3MiOiJodHRwczovL2Nsb3VkLmdvb2dsZS5jb20vaWFwIiwic3ViIjoiYWNjb3VudHMuZ29vZ2xlLmNvbToxMTIxODE3MTI3NzEyMDE5NzI4OTEifQ.yKNtdFY5EKkRboYNexBdfugzLhC3VuGyFcuFYA8kgpxMqfyxa41zkML68hYKrWu2kOBTUW95UnbGpsIi_u1fiA";

  private static final String FEDERATED_SIGNON_RS256_TOKEN =
      "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY5ZDk3YjRjYWU5MGJjZDc2YWViMjAwMjZmNmI3NzBjYWMyMjE3ODMiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJodHRwczovL2V4YW1wbGUuY29tL3BhdGgiLCJhenAiOiJpbnRlZ3JhdGlvbi10ZXN0c0BjaGluZ29yLXRlc3QuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLCJlbWFpbCI6ImludGVncmF0aW9uLXRlc3RzQGNoaW5nb3ItdGVzdC5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJleHAiOjE1ODc2Mjk4ODgsImlhdCI6MTU4NzYyNjI4OCwiaXNzIjoiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tIiwic3ViIjoiMTA0MDI5MjkyODUzMDk5OTc4MjkzIn0.Pj4KsJh7riU7ZIbPMcHcHWhasWEcbVjGP4yx_5E0iOpeDalTdri97E-o0dSSkuVX2FeBIgGUg_TNNgJ3YY97T737jT5DUYwdv6M51dDlLmmNqlu_P6toGCSRC8-Beu5gGmqS2Y82TmpHH9Vhoh5PsK7_rVHk8U6VrrVVKKTWm_IzTFhqX1oYKPdvfyaNLsXPbCt_NFE0C3DNmFkgVhRJu7LtzQQN-ghaqd3Ga3i6KH222OEI_PU4BUTvEiNOqRGoMlT_YOsyFN3XwqQ6jQGWhhkArL1z3CG2BVQjHTKpgVsRyy_H6WTZiju2Q-XWobgH-UPSZbyymV8-cFT9XKEtZQ";
  private static final String LEGACY_FEDERATED_SIGNON_CERT_URL =
      "https://www.googleapis.com/oauth2/v1/certs";

  private static final String SERVICE_ACCOUNT_RS256_TOKEN =
      "eyJhbGciOiJSUzI1NiIsImtpZCI6IjJlZjc3YjM4YTFiMDM3MDQ4NzA0MzkxNmFjYmYyN2Q3NGVkZDA4YjEiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJodHRwczovL2V4YW1wbGUuY29tL2F1ZGllbmNlIiwiZXhwIjoxNTg3NjMwNTQzLCJpYXQiOjE1ODc2MjY5NDMsImlzcyI6InNvbWUgaXNzdWVyIiwic3ViIjoic29tZSBzdWJqZWN0In0.gGOQW0qQgs4jGUmCsgRV83RqsJLaEy89-ZOG6p1u0Y26FyY06b6Odgd7xXLsSTiiSnch62dl0Lfi9D0x2ByxvsGOCbovmBl2ZZ0zHr1wpc4N0XS9lMUq5RJQbonDibxXG4nC2zroDfvD0h7i-L8KMXeJb9pYwW7LkmrM_YwYfJnWnZ4bpcsDjojmPeUBlACg7tjjOgBFbyQZvUtaERJwSRlaWibvNjof7eCVfZChE0PwBpZc_cGqSqKXv544L4ttqdCnmONjqrTATXwC4gYxruevkjHfYI5ojcQmXoWDJJ0-_jzfyPE4MFFdCFgzLgnfIOwe5ve0MtquKuv2O0pgvg";
  private static final String SERVICE_ACCOUNT_CERT_URL =
      "https://www.googleapis.com/robot/v1/metadata/x509/integration-tests%40chingor-test.iam.gserviceaccount.com";

  private static final List<String> ALL_TOKENS =
      Arrays.asList(ES256_TOKEN, FEDERATED_SIGNON_RS256_TOKEN, SERVICE_ACCOUNT_RS256_TOKEN);

  static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  static final MockClock FIXED_CLOCK = new MockClock(1584047020000L);

  private static IdToken newIdToken(String issuer, String audience) {
    Payload payload = new Payload();
    payload.setIssuer(issuer);
    payload.setAudience(audience);
    payload.setExpirationTimeSeconds(2000L);
    payload.setIssuedAtTimeSeconds(1000L);
    return new IdToken(new Header(), payload, new byte[0], new byte[0]);
  }

  public void testBuilder() throws Exception {
    IdTokenVerifier.Builder builder =
        new IdTokenVerifier.Builder().setIssuer(ISSUER).setAudience(TRUSTED_CLIENT_IDS);
    assertEquals(Clock.SYSTEM, builder.getClock());
    assertEquals(ISSUER, builder.getIssuer());
    assertEquals(Collections.singleton(ISSUER), builder.getIssuers());
    assertEquals(TRUSTED_CLIENT_IDS, builder.getAudience());
    Clock clock = new MockClock();
    builder.setClock(clock);
    assertEquals(clock, builder.getClock());
    IdTokenVerifier verifier = builder.build();
    assertEquals(clock, verifier.getClock());
    assertEquals(ISSUER, verifier.getIssuer());
    assertEquals(Collections.singleton(ISSUER), builder.getIssuers());
    assertEquals(TRUSTED_CLIENT_IDS, Lists.newArrayList(verifier.getAudience()));
  }

  public void testVerify() throws Exception {
    MockClock clock = new MockClock();
    MockEnvironment testEnvironment = new MockEnvironment();
    testEnvironment.setVariable(IdTokenVerifier.SKIP_SIGNATURE_ENV_VAR, "true");
    IdTokenVerifier verifier =
        new IdTokenVerifier.Builder()
            .setIssuers(Arrays.asList(ISSUER, ISSUER3))
            .setAudience(Arrays.asList(CLIENT_ID))
            .setClock(clock)
            .setEnvironment(testEnvironment)
            .build();

    // verifier flexible doesn't check issuer and audience
    IdTokenVerifier verifierFlexible =
        new IdTokenVerifier.Builder().setClock(clock).setEnvironment(testEnvironment).build();

    // issuer
    clock.timeMillis = 1500000L;
    IdToken idToken = newIdToken(ISSUER, CLIENT_ID);
    assertTrue(verifier.verify(idToken));
    assertTrue(verifierFlexible.verify(newIdToken(ISSUER2, CLIENT_ID)));
    assertFalse(verifier.verify(newIdToken(ISSUER2, CLIENT_ID)));
    assertTrue(verifier.verify(newIdToken(ISSUER3, CLIENT_ID)));
    // audience
    assertTrue(verifierFlexible.verify(newIdToken(ISSUER, CLIENT_ID2)));
    assertFalse(verifier.verify(newIdToken(ISSUER, CLIENT_ID2)));
    // time
    clock.timeMillis = 700000L;
    assertTrue(verifier.verify(idToken));
    clock.timeMillis = 2300000L;
    assertTrue(verifier.verify(idToken));
    clock.timeMillis = 699999L;
    assertFalse(verifier.verify(idToken));
    clock.timeMillis = 2300001L;
    assertFalse(verifier.verify(idToken));
  }

  public void testEmptyIssuersFails() throws Exception {
    IdTokenVerifier.Builder builder = new IdTokenVerifier.Builder();
    try {
      builder.setIssuers(Collections.<String>emptyList());
      fail("Exception expected");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }

  public void testBuilderSetNullIssuers() throws Exception {
    IdTokenVerifier.Builder builder = new IdTokenVerifier.Builder();
    IdTokenVerifier verifier = builder.build();
    assertNull(builder.getIssuers());
    assertNull(builder.getIssuer());
    assertNull(verifier.getIssuers());
    assertNull(verifier.getIssuer());

    builder.setIssuers(null);
    verifier = builder.build();
    assertNull(builder.getIssuers());
    assertNull(builder.getIssuer());
    assertNull(verifier.getIssuers());
    assertNull(verifier.getIssuer());

    builder.setIssuer(null);
    verifier = builder.build();
    assertNull(builder.getIssuers());
    assertNull(builder.getIssuer());
    assertNull(verifier.getIssuers());
    assertNull(verifier.getIssuer());
  }

  public void testMissingAudience() throws VerificationException {
    IdToken idToken = newIdToken(ISSUER, null);

    MockClock clock = new MockClock();
    clock.timeMillis = 1500000L;
    IdTokenVerifier verifier =
        new IdTokenVerifier.Builder()
            .setIssuers(Arrays.asList(ISSUER, ISSUER3))
            .setAudience(Collections.<String>emptyList())
            .setClock(clock)
            .build();
    assertFalse(verifier.verify(idToken));
  }

  public void testVerifyEs256TokenPublicKeyMismatch() throws Exception {
    // Mock HTTP requests
    HttpTransportFactory httpTransportFactory =
        new HttpTransportFactory() {
          @Override
          public HttpTransport create() {
            return new MockHttpTransport() {
              @Override
              public LowLevelHttpRequest buildRequest(String method, String url)
                  throws IOException {
                return new MockLowLevelHttpRequest() {
                  @Override
                  public LowLevelHttpResponse execute() throws IOException {
                    MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                    response.setStatusCode(200);
                    response.setContentType("application/json");
                    response.setContent("");
                    return response;
                  }
                };
              }
            };
          }
        };
    IdTokenVerifier tokenVerifier =
        new IdTokenVerifier.Builder()
            .setClock(FIXED_CLOCK)
            .setHttpTransportFactory(httpTransportFactory)
            .build();

    try {
      tokenVerifier.verifySignature(IdToken.parse(JSON_FACTORY, ES256_TOKEN));
      fail("Should have failed verification");
    } catch (VerificationException ex) {
      assertTrue(ex.getMessage().contains("Error fetching PublicKey"));
    }
  }

  public void testVerifyEs256Token() throws VerificationException, IOException {
    HttpTransportFactory httpTransportFactory =
        mockTransport(
            "https://www.gstatic.com/iap/verify/public_key-jwk",
            readResourceAsString("iap_keys.json"));
    IdTokenVerifier tokenVerifier =
        new IdTokenVerifier.Builder()
            .setClock(FIXED_CLOCK)
            .setHttpTransportFactory(httpTransportFactory)
            .build();
    assertTrue(tokenVerifier.verify(IdToken.parse(JSON_FACTORY, ES256_TOKEN)));
  }

  public void testVerifyRs256Token() throws VerificationException, IOException {
    HttpTransportFactory httpTransportFactory =
        mockTransport(
            "https://www.googleapis.com/oauth2/v3/certs",
            readResourceAsString("federated_keys.json"));
    MockClock clock = new MockClock(1587625988000L);
    IdTokenVerifier tokenVerifier =
        new IdTokenVerifier.Builder()
            .setClock(clock)
            .setHttpTransportFactory(httpTransportFactory)
            .build();
    assertTrue(tokenVerifier.verify(IdToken.parse(JSON_FACTORY, FEDERATED_SIGNON_RS256_TOKEN)));
  }

  public void testVerifyRs256TokenWithLegacyCertificateUrlFormat()
      throws VerificationException, IOException {
    HttpTransportFactory httpTransportFactory =
        mockTransport(
            LEGACY_FEDERATED_SIGNON_CERT_URL, readResourceAsString("legacy_federated_keys.json"));
    MockClock clock = new MockClock(1587626288000L);
    IdTokenVerifier tokenVerifier =
        new IdTokenVerifier.Builder()
            .setCertificatesLocation(LEGACY_FEDERATED_SIGNON_CERT_URL)
            .setClock(clock)
            .setHttpTransportFactory(httpTransportFactory)
            .build();
    assertTrue(tokenVerifier.verify(IdToken.parse(JSON_FACTORY, FEDERATED_SIGNON_RS256_TOKEN)));
  }

  public void testVerifyServiceAccountRs256Token() throws VerificationException, IOException {
    MockClock clock = new MockClock(1587626643000L);
    IdTokenVerifier tokenVerifier =
        new IdTokenVerifier.Builder()
            .setClock(clock)
            .setCertificatesLocation(SERVICE_ACCOUNT_CERT_URL)
            .setHttpTransportFactory(new DefaultHttpTransportFactory())
            .build();
    assertTrue(tokenVerifier.verify(IdToken.parse(JSON_FACTORY, SERVICE_ACCOUNT_RS256_TOKEN)));
  }

  static String readResourceAsString(String resourceName) throws IOException {
    InputStream inputStream =
        IdTokenVerifierTest.class.getClassLoader().getResourceAsStream(resourceName);
    try (final Reader reader = new InputStreamReader(inputStream)) {
      return CharStreams.toString(reader);
    }
  }

  static HttpTransportFactory mockTransport(String url, String certificates) {
    final String certificatesContent = certificates;
    final String certificatesUrl = url;
    return new HttpTransportFactory() {
      @Override
      public HttpTransport create() {
        return new MockHttpTransport() {
          @Override
          public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
            assertEquals(certificatesUrl, url);
            return new MockLowLevelHttpRequest() {
              @Override
              public LowLevelHttpResponse execute() throws IOException {
                MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
                response.setStatusCode(200);
                response.setContentType("application/json");
                response.setContent(certificatesContent);
                return response;
              }
            };
          }
        };
      }
    };
  }

  /** A mock implementation of {@link Clock} to set clock for testing */
  static class MockClock implements Clock {
    public MockClock() {}

    public MockClock(long timeMillis) {
      this.timeMillis = timeMillis;
    }

    long timeMillis;

    public long currentTimeMillis() {
      return timeMillis;
    }
  }

  /** A default http transport factory for testing */
  static class DefaultHttpTransportFactory implements HttpTransportFactory {
    public HttpTransport create() {
      return new NetHttpTransport();
    }
  }

  /** A mock implementation of {@link Environment} to set environment variables for testing */
  class MockEnvironment extends Environment {
    private final Map<String, String> variables = new HashMap<>();

    @Override
    public String getVariable(String name) {
      return variables.get(name);
    }

    public void setVariable(String name, String value) {
      variables.put(name, value);
    }
  }
}
