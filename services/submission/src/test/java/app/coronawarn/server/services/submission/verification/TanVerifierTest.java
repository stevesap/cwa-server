/*
 * Corona-Warn-App
 *
 * SAP SE and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package app.coronawarn.server.services.submission.verification;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import app.coronawarn.server.services.submission.config.SubmissionServiceConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;

@EnableConfigurationProperties(value = SubmissionServiceConfig.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TanVerifier.class},
  initializers = ConfigFileApplicationContextInitializer.class)
@RestClientTest
public class TanVerifierTest {

  @Autowired
  private MockRestServiceServer server;

  @Autowired
  private TanVerifier tanVerifier;

  @Autowired
  private SubmissionServiceConfig submissionServiceConfig;

  private String verificationUrl;
  private String randomUUID;

  @BeforeEach
  public void setup() {
    this.verificationUrl = submissionServiceConfig.getVerificationBaseUrl()
      + submissionServiceConfig.getVerificationPath();
    this.randomUUID = UUID.randomUUID().toString();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "ANY SYNTAX", "123456", "ABCD23X", "ZZZZZZZ", "Bearer 3123fe", "", "&%$§&%&$%/%&",
    "LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOONG"
  })
  public void checkWrongTanSyntax(String invalidSyntaxTan) {
    assertThat(tanVerifier.verifyTan(invalidSyntaxTan)).isFalse();
  }

  @Test
  public void checkValidTan() {
    this.server
      .expect(ExpectedCount.once(), requestTo(verificationUrl))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(HttpStatus.OK));
    assertThat(tanVerifier.verifyTan(randomUUID)).isTrue();
  }

  @Test
  public void checkInvalidTan() {
    this.server
      .expect(ExpectedCount.once(), requestTo(verificationUrl))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));
    assertThat(tanVerifier.verifyTan(randomUUID)).isFalse();
  }

  @Test
  public void checkInternalServerError() {
    this.server
      .expect(ExpectedCount.once(), requestTo(verificationUrl))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() -> tanVerifier.verifyTan(randomUUID));
  }
}
