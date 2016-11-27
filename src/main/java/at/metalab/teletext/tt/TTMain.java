package at.metalab.teletext.tt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import at.metalab.teletext.Teletext;

public class TTMain {

	public final static ObjectMapper OM = new ObjectMapper();

	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class TTJson {

		public StatusJson[] sitzklo;

		public StatusJson[] stehklo;

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

		final String template200 = IOUtils.toString(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("R20000.TTIx"), "US-ASCII");

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
			System.out.println("starting to track the toilet status");

			String dispCyber = " UNKNOWN";
			String dispSteh = " UNKNOWN";

			String fDispCyber = Teletext.WHITE + Teletext.DOUBLE;
			String fDispSteh = Teletext.WHITE + Teletext.DOUBLE;

			while (true) {
				try {
					String responseBody = httpclient.execute(httpget, responseHandler);
					TTJson tt = TTJson.parse(responseBody);

					dispCyber = getStatusText(tt.sitzklo);
					fDispCyber = getStatusFormat(tt.sitzklo);

					dispSteh = getStatusText(tt.stehklo);
					fDispSteh = getStatusFormat(tt.stehklo);

					String renderedPage = template200
							.replace("STITLE", Teletext.DOUBLE + Teletext.YELLOW)
							.replace("HCYBER", Teletext.DOUBLE + Teletext.CYAN)
							.replace("SCYBER", fDispCyber)
							.replace("HSTEH", Teletext.DOUBLE + Teletext.CYAN)
							.replace("SSTEH", fDispSteh)
							.replace("CCCCCCCCCCCCCC", dispCyber)
							.replace("SSSSSSSSSSSSSS", dispSteh)
							.replace("TTTTTTTTTTTTTTTTTTT",
									new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					IOUtils.write(renderedPage, bos, "ISO-8859-1");

					Teletext.scp(bos.toByteArray(), "/home/pi/Pages", "R20000.TTIx");
					System.out.println("updated page");
				} catch (ClientProtocolException cpe) {
					cpe.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

				Thread.sleep(5 * 1000);
			}
		} finally {
			httpclient.close();
		}
	}
	
	public static String getStatusText(StatusJson[] statusJson) {
		try {
			String s = statusJson[0].status;

			if ("open".equals(s)) {
				return " AVAILABLE";
			} else if ("closed".equals(s)) {
				return "OCCUPIED";
			} else {
				return " UNKNOWN";
			}
		} catch (Exception exception) {
			return " UNKNOWN";
		}
	}
	
	public static String getStatusFormat(StatusJson[] statusJson) {
		try {
			String s = statusJson[0].status;

			if ("open".equals(s)) {
				return Teletext.GREEN + Teletext.DOUBLE;
			} else if ("closed".equals(s)) {
				return Teletext.YELLOW + Teletext.DOUBLE + Teletext.BLINK;
			} else {
				return Teletext.WHITE + Teletext.DOUBLE;
			}
		} catch (Exception exception) {
			return Teletext.WHITE + Teletext.DOUBLE;
		}
	}

}
