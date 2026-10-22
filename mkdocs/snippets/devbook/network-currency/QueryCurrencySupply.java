//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class QueryCurrencySupply {
	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final String NODE_URL = System.getenv().getOrDefault(
		"NODE_URL", "http://portobelo.nemmain.net:7890");

	private static String fmtAtomic(
		final BigInteger atomic,
		final BigInteger scale
	) {
		final BigInteger[] parts = atomic.divideAndRemainder(scale);
		final int divisibility = scale.toString().length() - 1;
		return String.format(Locale.US,
			"%,d.%0" + divisibility + "d", parts[0], parts[1]);
	}

	public static void main(final String[] args) {
		try {
			new QueryCurrencySupply().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", NODE_URL);

		final String mosaicId = "nem:xem"; // [>step-1]
		final String supplyPath = String.format(
			"/mosaic/supply?mosaicId=%s", mosaicId);
		final HttpRequest supplyRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + supplyPath)).GET().build();
		final HttpResponse<String> supplyResponse = HTTP_CLIENT.send(
			supplyRequest, BodyHandlers.ofString());
		final JsonNode supplyInfo = JSON_MAPPER.readTree(
			supplyResponse.body());
		final BigInteger totalSupply = new BigInteger(
			supplyInfo.get("supply").asText());
		System.out.printf("Total supply: %,d %s%n",
			totalSupply, mosaicId); // [<step-1]
		// Read the mosaic's divisibility to convert balances [>step-2]
		// to whole units
		final String definitionPath = String.format(
			"/mosaic/definition?mosaicId=%s", mosaicId);
		final HttpRequest definitionRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + definitionPath)).GET().build();
		final HttpResponse<String> definitionResponse = HTTP_CLIENT.send(
			definitionRequest, BodyHandlers.ofString());
		final JsonNode definition = JSON_MAPPER.readTree(
			definitionResponse.body());
		int divisibility = 0;
		for (final JsonNode property : definition.get("properties")) {
			if ("divisibility".equals(property.get("name").asText()))
				divisibility = property.get("value").asInt();
		} // [<step-2]
		// [>step-3]
		final BigInteger scale = BigInteger.TEN.pow(divisibility);
		final List<List<String>> nonCirculatingAddresses = List.of(
			List.of("Treasury",
				"NCHESTYVD2P6P646AMY7WSNG73PCPZDUQNSD6JAK"),
			List.of("Nemesis",
				"NANEMOABLAGR72AZ2RV3V4ZHDCXW25XQ73O7OBT5"),
			List.of("Namespace rental",
				"NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA"),
			List.of("Mosaic rental",
				"NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS"));
		BigInteger nonCirculatingSupply = BigInteger.ZERO;
		for (final List<String> entry : nonCirculatingAddresses) {
			final String accountPath = String.format(
				"/account/get?address=%s", entry.get(1));
			final HttpRequest accountRequest = HttpRequest.newBuilder(
				URI.create(NODE_URL + accountPath)).GET().build();
			final HttpResponse<String> accountResponse = HTTP_CLIENT.send(
				accountRequest, BodyHandlers.ofString());
			final BigInteger balance = new BigInteger(JSON_MAPPER.readTree(
				accountResponse.body()).get("account").get("balance")
				.asText());
			nonCirculatingSupply = nonCirculatingSupply.add(balance);
			System.out.printf("  %s: %s %s%n", entry.get(0),
				fmtAtomic(balance, scale), mosaicId);
		}
		System.out.printf("Non-circulating supply: %s %s%n",
			fmtAtomic(nonCirculatingSupply, scale),
			mosaicId); // [<step-3]
		// [>step-4]
		final BigInteger circulatingSupply = totalSupply.multiply(scale)
			.subtract(nonCirculatingSupply);
		System.out.printf("Circulating supply: %s %s%n",
			fmtAtomic(circulatingSupply, scale),
			mosaicId); // [<step-4]
	}
}
