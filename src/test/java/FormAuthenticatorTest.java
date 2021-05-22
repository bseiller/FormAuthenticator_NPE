import org.apache.catalina.Connector;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Response;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.core.DummyRequest;
import org.apache.catalina.core.DummyResponse;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StandardSession;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Random;

public class FormAuthenticatorTest {
	// as defined/initialized in org.apache.catalina.connector.Connector.maxSavePostSize
	private static final int DEFAULT_MAX_SAVE_POST_SIZE = 4096;

	private OpenFormAuthenticator subject;

	@Before
    public void init() {
        subject = new OpenFormAuthenticator();
    }

    /**
     * NullPointerException for default maxSavePostSize of 4096 and requests that have a body size of exactly 4096 bytes.
     * This results in requests with a valid size not being saved properly.
     * Also those requests won't show up in the access logs.
     */
    @Test
    public void testSaveRequestNPESameSizeRequest() throws IOException {
        final HttpRequest request = new TestDummyHttpRequest(DEFAULT_MAX_SAVE_POST_SIZE, DEFAULT_MAX_SAVE_POST_SIZE);
        subject._saveRequest(request);
    }

    /**
     * NullPointerException for custom maxSavePostSize smaller than 4096 and requests that have a body size matching the custom maxSavePostSize
     * This results in requests with a valid size not being saved properly.
     * Also those requests won't show up in the access logs.
     */
    @Test
    public void testSaveRequestSmallerMaxAndRequestSize() throws IOException {
        final int smallerMaxSavePostSize = DEFAULT_MAX_SAVE_POST_SIZE - 1;
        final HttpRequest request = new TestDummyHttpRequest(smallerMaxSavePostSize, smallerMaxSavePostSize);
        subject._saveRequest(request);
    }

    /**
     * NullPointerException for default maxSavePostSize of 4096 and requests that have a body size larger than 4096 bytes.
     * Those requests won't show up in the access logs, but wouldn't be saved as they are to big anyway.
     */
    @Test
    public void testSaveRequestNPELargerRequest() throws IOException {
        final HttpRequest request = new TestDummyHttpRequest(DEFAULT_MAX_SAVE_POST_SIZE, DEFAULT_MAX_SAVE_POST_SIZE + 1);
        subject._saveRequest(request);
    }

    /**
     * no exception for default maxSavePostSize and requests that have a body size smaller than 4096 bytes
     */
    @Test
    public void testSaveRequestSmallerRequest() throws IOException {
        final HttpRequest request = new TestDummyHttpRequest(DEFAULT_MAX_SAVE_POST_SIZE, DEFAULT_MAX_SAVE_POST_SIZE - 1);
        subject._saveRequest(request);
    }

    /**
     * IOException for a larger than default maxSavePostSize (> 4096) and requests that have a body size larger than the custom maxSavePostSize
     */
    @Test
    //@Test(expected = IOException.class)
    public void testSaveRequestIOExceptionFlushBuffer() throws IOException {
        final int maxSavePostSize = DEFAULT_MAX_SAVE_POST_SIZE + 1;
        final HttpRequest request = new TestDummyHttpRequest(maxSavePostSize, maxSavePostSize + 1);

        subject._saveRequest(request);
    }

    /**
     * no exception for larger than default maxSavePostSize and requests that have a body size matching the custom maxSavePostSize
     */
    @Test
    public void testSaveRequestLargerMaxAndRequestSize() throws IOException {
        final int maxSavePostSize = DEFAULT_MAX_SAVE_POST_SIZE + 1;
        final HttpRequest request = new TestDummyHttpRequest(maxSavePostSize, maxSavePostSize);

        subject._saveRequest(request);
    }

    /**
     * no exception for larger than default maxSavePostSize and requests that have a body size smaller than the custom maxSavePostSize
     */
    @Test
    public void testSaveRequestNoException() throws IOException {
        final HttpRequest request = new TestDummyHttpRequest(DEFAULT_MAX_SAVE_POST_SIZE + 1, DEFAULT_MAX_SAVE_POST_SIZE);
        subject._saveRequest(request);
    }

	/**
     * to get public access to saveRequest
	 */ 
    private static class OpenFormAuthenticator extends FormAuthenticator {
        public void _saveRequest(HttpRequest request) throws IOException {
            super.saveRequest(request, new StandardSession(new StandardManager()));
        }
    }

    private static class TestDummyHttpRequest extends DummyRequest {

        private final org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector();
        private final Response response = new DummyResponse();
        private final InputStream inputStream;

        public TestDummyHttpRequest(final int maxSavePostSize, final int postRequestBodySize) {
            connector.setMaxSavePostSize(maxSavePostSize);
            inputStream = new ByteArrayInputStream(TestDummyHttpRequest.getRandomBytes(postRequestBodySize));
        }

        @Override
        public Connector getConnector() {
            return connector;
        }

        @Override
        public Response getResponse() {
            return response;
        }

        @Override
        public InputStream getStream() {
            return inputStream;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return Collections.emptyEnumeration();
        }

        private static byte[] getRandomBytes(int targetStringLength) {
            byte[] array = new byte[targetStringLength];
            new Random().nextBytes(array);
            return array;
        }
    }
}
