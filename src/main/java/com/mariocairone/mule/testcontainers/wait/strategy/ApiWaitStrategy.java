package com.mariocairone.mule.testcontainers.wait.strategy;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.com.google.common.base.Strings;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

public class ApiWaitStrategy extends AbstractWaitStrategy {

	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiWaitStrategy.class);
	/**
	 * Authorization HTTP header.
	 */
	private static final String HEADER_AUTHORIZATION = "Authorization";
	/**
	 * Basic Authorization scheme prefix.
	 */
	private static final String AUTH_BASIC = "Basic ";
	private String path = "/";
	private Set<Integer> statusCodes = new HashSet<>();
	private boolean tlsEnabled;
	private String username;
	private String password;
	private Predicate<String> responsePredicate;
	private Predicate<Integer> statusCodePredicate = null;
	private Optional<Integer> livenessPort = Optional.empty();
	private String authorization;

	private Map<String, String> queryParams = new HashMap<>();

	private Map<String, String> headers = new HashMap<>();

	private String body;
	private String method = "GET";

	/**
	 * Waits for the given status code.
	 *
	 * @param statusCode the expected status code
	 * @return this
	 */
	public ApiWaitStrategy forStatusCode(int statusCode) {
		statusCodes.add(statusCode);
		return this;
	}

	/**
	 * Waits for the status code to pass the given predicate
	 * 
	 * @param statusCodePredicate The predicate to test the response against
	 * @return this
	 */
	public ApiWaitStrategy forStatusCodeMatching(Predicate<Integer> statusCodePredicate) {
		this.statusCodePredicate = statusCodePredicate;
		return this;
	}

	/**
	 * Waits for the given path.
	 *
	 * @param path the path to check
	 * @return this
	 */
	public ApiWaitStrategy forPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Wait for the given port.
	 *
	 * @param port the given port
	 * @return this
	 */
	public ApiWaitStrategy forPort(int port) {
		this.livenessPort = Optional.of(port);
		return this;
	}

	/**
	 * Indicates that the status check should use HTTPS.
	 *
	 * @return this
	 */
	public ApiWaitStrategy usingTls() {
		this.tlsEnabled = true;
		return this;
	}

	/**
	 * Indicates that the status check should trust All certificates.
	 *
	 * @return this
	 */
	public ApiWaitStrategy usingRelaxedTls() {
		trustAllCertificates();
		return usingTls();
	}

	/**
	 * Authenticate with HTTP Basic Authorization credentials.
	 *
	 * @param username the username
	 * @param password the password
	 * @return this
	 */
	public ApiWaitStrategy withBasicCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		return this;
	}

	/**
	 * Authenticate with HTTP Authorization header.
	 *
	 * @param authorization the authorization
	 * @return this
	 */
	public ApiWaitStrategy withAuthorization(String authorization) {
		this.authorization = authorization;
		return this;
	}

	/**
	 * Add HTTP Query Parameters.
	 *
	 * @param name the query parameter name
	 * @param value the query parameter value
	 * @return this
	 */
	public ApiWaitStrategy withQueryParam(String name, String value) {
		this.queryParams.put(name, value);
		return this;
	}

	/**
	 * Add HTTP headers.
	 *
	 * @param name the http header name
	 * @param value the http header value
	 * @return this
	 */
	public ApiWaitStrategy withHeader(String name, String value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * Add HTTP body.
	 *
	 * @param body the http body
	 * @return this
	 */
	public ApiWaitStrategy withBody(String body) {
		this.body = body;
		return this;
	}

	/**
	 * Add HTTP method.
	 *
	 * @param method the http method
	 * @return this
	 */
	public ApiWaitStrategy withMethod(String method) {
		this.method = method;
		return this;
	}

	/**
	 * Waits for the response to pass the given predicate
	 * 
	 * @param responsePredicate The predicate to test the response against
	 * @return this
	 */
	public ApiWaitStrategy forResponsePredicate(Predicate<String> responsePredicate) {
		this.responsePredicate = responsePredicate;
		return this;
	}

	@Override
	protected void waitUntilReady() {
		final String containerName = waitStrategyTarget.getContainerInfo().getName();
		final Integer livenessCheckPort = livenessPort.map(waitStrategyTarget::getMappedPort).orElseGet(() -> {
			final Set<Integer> livenessCheckPorts = getLivenessCheckPorts();
			if (livenessCheckPorts == null || livenessCheckPorts.isEmpty()) {
				log.warn("{}: No exposed ports or mapped ports - cannot wait for status", containerName);
				return -1;
			}
			return livenessCheckPorts.iterator().next();
		});
		if (null == livenessCheckPort || -1 == livenessCheckPort) {
			return;
		}
		final String baseUri = buildLivenessUri(livenessCheckPort).toString();
		final URI uri = applyQueryParams(baseUri);
		log.info("{}: Waiting for {} seconds for URL: {}", containerName, startupTimeout.getSeconds(), uri.toString());
		// try to connect to the URL
		try {
			retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
				getRateLimiter().doWhenReady(() -> {
					try {
						// URI uri = applyQueryParams(baseUri);
						System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
						final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
						// authenticate
						if (!Strings.isNullOrEmpty(authorization)) {
							connection.setRequestProperty(HEADER_AUTHORIZATION, authorization);
							connection.setUseCaches(false);
						} else if (!Strings.isNullOrEmpty(username)) {
							connection.setRequestProperty(HEADER_AUTHORIZATION, buildAuthString(username, password));
							connection.setUseCaches(false);
						}
						connection.setRequestMethod(method);

						setHttpHeaders(connection);

						sendBody(connection);

						connection.connect();
						log.trace("Get response code {}", connection.getResponseCode());
						// Choose the statusCodePredicate strategy depending on what we defined.
						Predicate<Integer> predicate;
						if (statusCodes.isEmpty() && statusCodePredicate == null) {
							// We have no status code and no predicate so we expect a 200 OK response code
							predicate = responseCode -> HttpURLConnection.HTTP_OK == responseCode;
						} else if (!statusCodes.isEmpty() && statusCodePredicate == null) {
							// We use the default status predicate checker when we only have status codes
							predicate = responseCode -> statusCodes.contains(responseCode);
						} else if (statusCodes.isEmpty()) {
							// We only have a predicate
							predicate = statusCodePredicate;
						} else {
							// We have both predicate and status code
							predicate = statusCodePredicate.or(responseCode -> statusCodes.contains(responseCode));
						}
						if (!predicate.test(connection.getResponseCode())) {
							throw new RuntimeException(
									String.format("HTTP response code was: %s", connection.getResponseCode()));
						}
						if (responsePredicate != null) {
							String responseBody = getResponseBody(connection);
							log.trace("Get response {}", responseBody);
							if (!responsePredicate.test(responseBody)) {
								throw new RuntimeException(
										String.format("Response: %s did not match predicate", responseBody));
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
				return true;
			});
		} catch (TimeoutException e) {
			throw new ContainerLaunchException(
					String.format("Timed out waiting for URL to be accessible (%s should return HTTP %s)", baseUri,
							statusCodes.isEmpty() ? HttpURLConnection.HTTP_OK : statusCodes));
		}
	}

	/**
	 * Build the URI on which to check if the container is ready.
	 *
	 * @param livenessCheckPort the liveness port
	 * @return the liveness URI
	 */
	private URI buildLivenessUri(int livenessCheckPort) {
		final String scheme = (tlsEnabled ? "https" : "http") + "://";
		final String host = waitStrategyTarget.getContainerIpAddress();
		final String portSuffix;
		if ((tlsEnabled && 443 == livenessCheckPort) || (!tlsEnabled && 80 == livenessCheckPort)) {
			portSuffix = "";
		} else {
			portSuffix = ":" + String.valueOf(livenessCheckPort);
		}
		return URI.create(scheme + host + portSuffix + path);
	}

	/**
	 * @param username the username
	 * @param password the password
	 * @return a basic authentication string for the given credentials
	 */
	private String buildAuthString(String username, String password) {
		return AUTH_BASIC + BaseEncoding.base64().encode((username + ":" + password).getBytes());
	}

	private String getResponseBody(HttpURLConnection connection) throws IOException {
		BufferedReader reader;
		if (200 <= connection.getResponseCode() && connection.getResponseCode() <= 299) {
			reader = new BufferedReader(new InputStreamReader((connection.getInputStream())));
		} else {
			reader = new BufferedReader(new InputStreamReader((connection.getErrorStream())));
		}
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		return builder.toString();
	}

	public void trustAllCertificates() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					X509Certificate[] myTrustedAnchors = new X509Certificate[0];
					return myTrustedAnchors;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			});
		} catch (Exception e) {
		}
	}

	private void setHttpHeaders(HttpURLConnection con) {
		headers.forEach((name, value) -> {
			con.setRequestProperty(name, value);
		});
	}

	private URI applyQueryParams(String baseUri) {

		URI uri = null;

		try {

			uri = new URI(baseUri);

			if (queryParams.isEmpty())
				return uri;

			StringBuilder query = new StringBuilder();
			queryParams.forEach((name, value) -> {
				if (query.length() > 0)
					query.append("&");
				try {
					query.append(URLEncoder.encode(name, "UTF-8")).append("=")
							.append(URLEncoder.encode(value, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					log.error(e.getMessage(), e);
				}

			});

			return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query.toString(), null);

		} catch (URISyntaxException e) {
			log.error(e.getMessage(), e);
		}

		return uri;

	}

	private void sendBody(HttpURLConnection con) throws IOException {
		if (body == null)
			return;
		con.setDoOutput(true);
		DataOutputStream wr = null;
		try {
			wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(body);
			wr.flush();
			wr.close();
		} catch (IOException exception) {
			throw exception;
		} finally {
			this.closeQuietly(wr);
		}
	}

	private void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ex) {

		}
	}

}
