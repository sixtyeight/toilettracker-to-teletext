package at.metalab.teletext.projects;

import java.io.File;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import at.metalab.teletext.Teletext;

public class ProjectsMain {

	private static class Project {

		public String name;

		public String teaser;

		public String uri;

	}

	public final static void main(String[] args) throws Exception {
		final File page300 = new File("/home/pi/Pages/R30000.TTIx");

		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, new TrustManager[] { new TrustAllX509TrustManager() }, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String string, SSLSession ssls) {
				return true;
			}
		});

		try {
			System.out.println("starting to update projects page now");

			while (true) {
				final List<Project> projects = new LinkedList<ProjectsMain.Project>();

				String template = IOUtils
						.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("R30000.TTIx"),
								"ISO-8859-1")
						.replace("STITLE", Teletext.YELLOW + Teletext.DOUBLE)
						.replace("NMA", Teletext.YELLOW)
						.replace("TMA", Teletext.CYAN)
						.replace("WMA", Teletext.WHITE);

				List<String> templateRows = IOUtils.readLines(new StringReader(template));

				Document doc = Jsoup.connect("https://metalab.at/project/").get();

				Elements projectEls = doc.select(".project");

				for (Element projectEl : projectEls) {
					Element linkEl = projectEl.select("a").first();
					Element smallEl = projectEl.select("small").first();

					if (linkEl == null || smallEl == null) {
						continue;
					}

					Project project = new Project();
					project.name = linkEl.html();

					if (project.name == null) {
						continue;
					}

					project.teaser = smallEl.html();
					project.uri = linkEl.attr("abs:href");

					projects.add(project);
				}

				Map<Integer, Map<String, String>> vars = new HashMap<Integer, Map<String, String>>();
				for (int i = 1; i < 25; i++) {
					vars.put(i, new HashMap<String, String>());
				}

				{
					int row = 6;
					for (Project project : projects) {
						vars.get(row).put("NAME", project.name);
						vars.get(row).put("TEASER", project.teaser);
						row++;

						vars.get(row).put("WIKI", project.uri);
						row++;

						row++;
					}
				}

				List<String> outputRows = new LinkedList<>();
				for (String templateRow : templateRows) {
					if (templateRow.startsWith("OL")) {
						Integer row = Integer.valueOf(templateRow.split(",")[1]);
						Map<String, String> replacements = vars.get(row);

						outputRows.add(templateRow.replace("NAME", StringUtils.defaultString(replacements.get("NAME")))
								.replace("TEASER", StringUtils.defaultString(replacements.get("TEASER")))
								.replace("WIKI", StringUtils.defaultString(replacements.get("WIKI"))));
					} else {
						outputRows.add(templateRow);
					}
				}

				String c = StringUtils.join(outputRows, "\n");

				System.out.println(c);

				try {
					Teletext.scp(c.getBytes("ISO-8859-1"), "/home/pi/Pages", "R30000.TTIx");
					System.out.println("updated");
				} catch (Exception exception) {
					System.out.println("update failed:");
					exception.printStackTrace();
				}

				Thread.sleep(60 * 1000 * 30);
			}
		} finally {
		}
	}
}
