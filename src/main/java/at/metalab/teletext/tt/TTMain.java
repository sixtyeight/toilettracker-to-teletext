package at.metalab.teletext.tt;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TTMain {

	public final static ObjectMapper OM = new ObjectMapper();

	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class TTJson {

		public StatusJson[] sitzklo;

		public static TTJson parse(String json) {
			try {
				return OM.readValue(json, TTJson.class);
			} catch (Exception exception) {
				throw new RuntimeException("parse failed", exception);
			}
		}

	}

	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class StatusJson {
		public String status;
	}

	public final static void main(String[] args) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();

		final HttpGet httpget = new HttpGet("http://10.20.30.85/status.json");

		final File page200 = new File("/home/pi/Pages/R20000.TTIx");
		final String template200 = IOUtils
				.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("R20000.TTIx"), "ASCII");

		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			@Override
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? EntityUtils.toString(entity) : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}
		};

		try {
			System.out.println("starting to update page now");

			while (true) {
				String displayStatus = "* UNKNOWN *";

				try {
					String responseBody = httpclient.execute(httpget, responseHandler);
					TTJson tt = TTJson.parse(responseBody);

					String status = tt.sitzklo[0].status;

					if ("open".equals(status)) {
						displayStatus = "OPEN";
					} else if ("closed".equals(status)) {
						displayStatus = "* CLOSED *";
					}
				} catch (ClientProtocolException cpe) {
					cpe.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

				String renderedPage = template200.replace("SSSSSSSSSSSSSS", displayStatus);

				FileUtils.write(page200, renderedPage, "ASCII");
				System.out.println("updated page: " + displayStatus);

				Thread.sleep(10 * 1000);
			}
		} finally {
			httpclient.close();
		}
	}
}
