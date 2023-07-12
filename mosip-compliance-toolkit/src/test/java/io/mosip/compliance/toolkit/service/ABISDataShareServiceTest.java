package io.mosip.compliance.toolkit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.compliance.toolkit.constants.AppConstants;
import io.mosip.compliance.toolkit.dto.abis.DataShareExpireRequest;
import io.mosip.compliance.toolkit.dto.abis.DataShareRequestDto;
import io.mosip.compliance.toolkit.dto.abis.DataShareResponseWrapperDto;
import io.mosip.compliance.toolkit.dto.abis.DataShareSaveTokenRequest;
import io.mosip.compliance.toolkit.entity.AbisDataShareTokenEntity;
import io.mosip.compliance.toolkit.repository.AbisDataShareTokenRepository;
import io.mosip.compliance.toolkit.repository.BiometricTestDataRepository;
import io.mosip.compliance.toolkit.util.KeyManagerHelper;
import io.mosip.compliance.toolkit.util.ObjectMapperConfig;
import io.mosip.kernel.core.authmanager.authadapter.model.AuthUserDetails;
import io.mosip.kernel.core.authmanager.authadapter.model.MosipUserDto;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
@WebMvcTest
public class ABISDataShareServiceTest {

    @InjectMocks
    private ABISDataShareService abisDataShareService;

    @Mock
    private TestCasesService testCasesService;

    @Mock
    private KeyManagerHelper keyManagerHelper;

    @Mock
    private Authentication authentication;

    @Mock
    SecurityContext securityContext;

    @Mock
    private ObjectStoreAdapter objectStore;

    @Mock
    private BiometricTestDataRepository biometricTestDataRepository;

    @Mock
    private ObjectMapperConfig objectMapperConfig;

    @Mock
    private AbisDataShareTokenRepository abisDataShareTokenRepository;

    @Mock
    private ObjectMapper mapper;

    private MosipUserDto mosipUserDto;

    @Before
    public void before() {
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        mosipUserDto = getMosipUserDto();
        AuthUserDetails authUserDetails = new AuthUserDetails(mosipUserDto, "token");
        Mockito.when(authentication.getPrincipal()).thenReturn(authUserDetails);
        SecurityContextHolder.setContext(securityContext);
    }

    /*
     * This class tests the authUserDetails method
     */
    @Test
    public void authUserDetailsTest() {
        ReflectionTestUtils.invokeMethod(abisDataShareService, "authUserDetails");
    }

    /*
     * This class tests the getPartnerId method
     */
    @Test
    public void getPartnerIdTest() {
        String result = ReflectionTestUtils.invokeMethod(abisDataShareService, "getPartnerId");
        Assert.assertEquals(mosipUserDto.getUserId(), result);
    }

    /*
     * This class tests the getDataShareUrl method
     */
    @Test
    public void getDataShareUrlTest() throws Exception {
        InputStream inputStream = mock(InputStream.class);
        Mockito.when(testCasesService.getPartnerTestDataStream(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(inputStream);
        Mockito.when(keyManagerHelper.getAuthToken()).thenReturn("authToken");

        DataShareRequestDto dataShareRequestDto = new DataShareRequestDto();
        dataShareRequestDto.setTestcaseId("ABIS3000");
        dataShareRequestDto.setBioTestDataName("testdata");
        dataShareRequestDto.setCbeffFileSuffix(1);

        ResponseWrapper<DataShareResponseWrapperDto> result = abisDataShareService.createDataShareUrl(dataShareRequestDto);

        assertNotNull(result);
    }

    /*
     * This class tests the expireDataShareUrl method
     */
    @Test
    public void expireDataShareUrlTest() {
        DataShareExpireRequest dataShareExpireRequest = new DataShareExpireRequest();
        dataShareExpireRequest.setUrl("wss://activemq.dev.mosip.net/ws");
        dataShareExpireRequest.setTransactionsAllowed(1);
        abisDataShareService.expireDataShareUrl(dataShareExpireRequest);
    }
    
    /*
     * These functions test the saveDataShareToken method
     */
    @Test
    public void testSaveDataShareToken_existingToken() {

        RequestWrapper<DataShareSaveTokenRequest> requestWrapper = new RequestWrapper<>();
        DataShareSaveTokenRequest dataShareSaveTokenRequest = new DataShareSaveTokenRequest();
        dataShareSaveTokenRequest.setPartnerId("1234");
        dataShareSaveTokenRequest.setCtkTestCaseId("5678");
        dataShareSaveTokenRequest.setCtkTestRunId("9012");
        dataShareSaveTokenRequest.setToken("token");
        requestWrapper.setRequest(dataShareSaveTokenRequest);

        AbisDataShareTokenEntity savedEntity = new AbisDataShareTokenEntity();
        savedEntity.setToken("token");
        savedEntity.setTestRunId("9012");
        savedEntity.setTestCaseId("5678");
        savedEntity.setPartnerId("1234");

        Mockito.when(abisDataShareTokenRepository.findByAllIds(
                Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(savedEntity));

        ResponseWrapper<String> responseWrapper = abisDataShareService.saveDataShareToken(requestWrapper);

        Mockito.verify(abisDataShareTokenRepository, Mockito.times(1)).updateResultByToken(
                AppConstants.SUCCESS, savedEntity.getToken());
        Assert.assertEquals(AppConstants.SUCCESS, responseWrapper.getResponse());
    }

    @Test
    public void testSaveDataShareToken_newToken() {
        RequestWrapper<DataShareSaveTokenRequest> requestWrapper = new RequestWrapper<>();
        DataShareSaveTokenRequest dataShareSaveTokenRequest = new DataShareSaveTokenRequest();
        dataShareSaveTokenRequest.setPartnerId("1234");
        dataShareSaveTokenRequest.setCtkTestCaseId("5678");
        dataShareSaveTokenRequest.setCtkTestRunId("9012");
        dataShareSaveTokenRequest.setToken("token");
        requestWrapper.setRequest(dataShareSaveTokenRequest);

        Mockito.when(abisDataShareTokenRepository.findByAllIds(
                Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());

        ResponseWrapper<String> responseWrapper = abisDataShareService.saveDataShareToken(requestWrapper);

        Mockito.verify(abisDataShareTokenRepository, Mockito.times(1)).save(
                Mockito.any(AbisDataShareTokenEntity.class));
    }

    /*
     * This method is used to get MosipUserDto in class
     */
    private MosipUserDto getMosipUserDto() {
        MosipUserDto mosipUserDto = new MosipUserDto();
        mosipUserDto.setUserId("123");
        mosipUserDto.setMail("abc@gmail.com");
        return mosipUserDto;
    }
}
