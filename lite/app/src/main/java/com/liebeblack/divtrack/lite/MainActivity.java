package com.liebeblack.divtrack.lite;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class MainActivity extends Activity {
    private static final String PREFERENCES = "lite_rate_cache";
    private static final String KEY_RATE = "official_rate";
    private static final String KEY_CHECKED_AT = "checked_at";
    private static final String KEY_SOURCE = "rate_source";

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private TextView rateValue;
    private TextView checkedAt;
    private TextView status;
    private Button refreshButton;
    private boolean isRefreshing;
    private volatile boolean activityDestroyed;
    private float preferredRateTextSizePx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rateValue = findViewById(R.id.rate_value);
        checkedAt = findViewById(R.id.checked_at);
        status = findViewById(R.id.status);
        refreshButton = findViewById(R.id.refresh_button);

        showCachedRate();
        fitRateTextToWidth();
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshRate();
            }
        });
        refreshRate();
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private void showCachedRate() {
        String cachedRate = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(KEY_RATE, null);
        long checkedAtMillis = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getLong(KEY_CHECKED_AT, 0L);

        if (cachedRate == null) {
            checkedAt.setText("");
            status.setText(R.string.status_no_cache);
            return;
        }

        BigDecimal parsedRate;
        try {
            parsedRate = new BigDecimal(cachedRate);
        } catch (NumberFormatException exception) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .remove(KEY_RATE)
                    .remove(KEY_SOURCE)
                    .remove(KEY_CHECKED_AT)
                    .apply();
            checkedAt.setText("");
            status.setText(R.string.status_cache_invalid);
            return;
        }
        if (parsedRate.signum() <= 0) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .remove(KEY_RATE)
                    .remove(KEY_SOURCE)
                    .remove(KEY_CHECKED_AT)
                    .apply();
            checkedAt.setText("");
            status.setText(R.string.status_cache_invalid);
            return;
        }

        String source = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getString(KEY_SOURCE, getString(R.string.source_bcv));
        rateValue.setText(formatRate(parsedRate));
        fitRateTextToWidth();
        if (checkedAtMillis > 0L) {
            checkedAt.setText(getString(
                    R.string.checked_at,
                    source,
                    formatTimestamp(checkedAtMillis)));
        }
        status.setText(R.string.status_cached);
    }

    private void refreshRate() {
        if (isRefreshing || isFinishing()) {
            return;
        }

        isRefreshing = true;
        refreshButton.setEnabled(false);
        refreshButton.setText(R.string.refreshing);
        status.setText(getSharedPreferences(PREFERENCES, MODE_PRIVATE).contains(KEY_RATE)
                ? R.string.status_cached
                : R.string.status_starting);
        status.setTextColor(getResources().getColor(R.color.lite_muted));

        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final BcvRateClient.RateQuote quote;
                try {
                    quote = BcvRateClient.fetchOfficialUsdRate();
                } catch (Exception exception) {
                    Log.w("DivTrackLite", "No se pudo actualizar la tasa BCV.", exception);
                    final int messageResource = getRefreshErrorMessage(exception);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!activityDestroyed) {
                                finishRefreshWithError(messageResource);
                            }
                        }
                    });
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activityDestroyed) {
                            return;
                        }
                        getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_RATE, quote.rate.toPlainString())
                                .putString(KEY_SOURCE, quote.source)
                                .putLong(KEY_CHECKED_AT, quote.updatedAtMillis)
                                .apply();
                        rateValue.setText(formatRate(quote.rate));
                        checkedAt.setText(getString(
                                R.string.checked_at,
                                quote.source,
                                formatTimestamp(quote.updatedAtMillis)));
                        if (quote.usedFallback) {
                            status.setText(getString(R.string.status_fallback_used, quote.source));
                        } else {
                            status.setText(R.string.status_updated);
                        }
                        fitRateTextToWidth();
                        finishRefresh();
                    }
                });
            }
        });
    }

    private int getRefreshErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        if (cause instanceof javax.net.ssl.SSLException) {
            return R.string.status_secure_connection_failed;
        }
        if (cause instanceof java.net.UnknownHostException) {
            return R.string.status_host_unavailable;
        }
        if (cause instanceof java.net.SocketTimeoutException) {
            return R.string.status_connection_timeout;
        }
        if (cause instanceof BcvRateClient.BcvResponseException) {
            return R.string.status_invalid_response;
        }
        return R.string.status_connection_failed;
    }

    private void finishRefreshWithError(int messageResource) {
        if (isFinishing()) {
            return;
        }
        boolean hasCachedRate = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .contains(KEY_RATE);
        status.setText(messageResource);
        if (hasCachedRate) {
            status.append("\n");
            status.append(getString(R.string.status_cache_preserved));
        }
        finishRefresh();
        status.setTextColor(getResources().getColor(R.color.lite_error));
    }

    private void finishRefresh() {
        isRefreshing = false;
        refreshButton.setEnabled(true);
        refreshButton.setText(R.string.refresh);
        status.setTextColor(getResources().getColor(R.color.lite_muted));
    }

    private void fitRateTextToWidth() {
        rateValue.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (rateValue.getViewTreeObserver().isAlive()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                rateValue.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                rateValue.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                        int availableWidth = rateValue.getWidth()
                                - rateValue.getPaddingLeft()
                                - rateValue.getPaddingRight();
                        if (availableWidth <= 0) {
                            return;
                        }

                        float baseSize = preferredRateTextSizePx;
                        if (baseSize <= 0f) {
                            baseSize = rateValue.getTextSize();
                            preferredRateTextSizePx = baseSize;
                        }
                        float measuredWidth = rateValue.getPaint()
                                .measureText(rateValue.getText().toString());
                        if (measuredWidth > availableWidth && measuredWidth > 0f) {
                            float adjustedSize = baseSize * availableWidth / measuredWidth;
                            float minimumSize = 24f * getResources()
                                    .getDisplayMetrics().scaledDensity;
                            rateValue.setTextSize(
                                    android.util.TypedValue.COMPLEX_UNIT_PX,
                                    Math.max(minimumSize, adjustedSize));
                        } else {
                            rateValue.setTextSize(
                                    android.util.TypedValue.COMPLEX_UNIT_PX,
                                    baseSize);
                        }
                    }
                });
    }

    private static String formatRate(BigDecimal rate) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "VE"));
        DecimalFormat format = new DecimalFormat("#,##0.0000##", symbols);
        format.setGroupingUsed(false);
        return format.format(rate);
    }

    private static String formatTimestamp(long timestampMillis) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "VE"));
        return format.format(new Date(timestampMillis));
    }
}

final class BcvRateClient {
    private static final String BCV_URL = "https://www.bcv.org.ve/";
    private static final String DOLAR_API_URL = "https://ve.dolarapi.com/v1/dolares";
    private static final String EXCHANGE_RATE_API_URL = "https://open.er-api.com/v6/latest/USD";
    private static final int CONNECT_TIMEOUT_MS = 12000;
    private static final int READ_TIMEOUT_MS = 12000;
    private static final int MAX_RESPONSE_CHARS = 1_500_000;

    private static final Pattern DOLLAR_SECTION = Pattern.compile(
            "(?is)<[^>]+\\bid\\s*=\\s*['\"]dolar['\"][^>]*>");
    private static final Pattern BCV_DATE = Pattern.compile(
            "(?is)Fecha\\s+Valor:.*?content\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern RATE_IN_STRONG = Pattern.compile(
            "(?is)<strong[^>]*>\\s*([0-9][0-9.,\\s]*)\\s*</strong>");
    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9.,]*");
    private static final Pattern USD_LABEL = Pattern.compile("(?i)\\bUSD\\b");

    private BcvRateClient() {
    }

    static RateQuote fetchOfficialUsdRate() throws java.io.IOException {
        try {
            return fetchBcvRate();
        } catch (java.io.IOException exception) {
            Log.w("DivTrackLite", "Falló BCV; se probará DolarAPI.", exception);
        }

        try {
            return fetchDolarApiRate();
        } catch (java.io.IOException exception) {
            Log.w("DivTrackLite", "Falló DolarAPI; se probará ER-API.", exception);
        }

        try {
            return fetchExchangeRateApiRate();
        } catch (java.io.IOException exception) {
            Log.w("DivTrackLite", "También falló ER-API.", exception);
            throw new java.io.IOException(
                    "Fallaron BCV, DolarAPI y ER-API; se conserva la última tasa guardada.",
                    exception);
        }
    }

    private static RateQuote fetchBcvRate() throws java.io.IOException {
        String html = readHttps(BCV_URL, "www.bcv.org.ve", "text/html");
        BigDecimal rate = parseDollarRate(html);
        Matcher sectionMatcher = DOLLAR_SECTION.matcher(html);
        if (!sectionMatcher.find()) {
            throw new BcvResponseException("No se encontró la sección de fecha del BCV.");
        }
        Matcher dateMatcher = BCV_DATE.matcher(html.substring(sectionMatcher.start()));
        long updatedAt = System.currentTimeMillis();
        if (dateMatcher.find()) {
            Long publishedAt = parseIsoTimestamp(dateMatcher.group(1));
            if (publishedAt != null) {
                updatedAt = publishedAt;
            }
        }
        return new RateQuote(rate, "BCV", updatedAt, false);
    }

    private static RateQuote fetchDolarApiRate() throws java.io.IOException {
        String body = readHttps(DOLAR_API_URL, "ve.dolarapi.com", "application/json");
        try {
            org.json.JSONArray rates = new org.json.JSONArray(body);
            for (int index = 0; index < rates.length(); index++) {
                org.json.JSONObject item = rates.optJSONObject(index);
                if (item == null
                        || !"oficial".equalsIgnoreCase(item.optString("fuente"))
                        || !"USD".equalsIgnoreCase(item.optString("moneda"))) {
                    continue;
                }

                BigDecimal rate = firstPositiveValue(item, "promedio", "venta", "compra");
                if (rate == null) {
                    throw new BcvResponseException("DolarAPI no publicó una tasa oficial utilizable.");
                }
                Long publishedAt = parseIsoTimestamp(item.optString("fechaActualizacion", ""));
                return new RateQuote(
                        rate,
                        "DolarAPI (respaldo)",
                        publishedAt != null ? publishedAt : System.currentTimeMillis(),
                        true);
            }
            throw new BcvResponseException("DolarAPI no devolvió el dólar oficial.");
        } catch (org.json.JSONException exception) {
            throw new BcvResponseException("La respuesta JSON de DolarAPI no es válida.", exception);
        }
    }

    private static RateQuote fetchExchangeRateApiRate() throws java.io.IOException {
        String body = readHttps(EXCHANGE_RATE_API_URL, "open.er-api.com", "application/json");
        try {
            org.json.JSONObject response = new org.json.JSONObject(body);
            if (!"success".equalsIgnoreCase(response.optString("result"))
                    || !"USD".equalsIgnoreCase(response.optString("base_code"))) {
                throw new BcvResponseException("ER-API no devolvió una tasa USD válida.");
            }

            org.json.JSONObject rates = response.optJSONObject("rates");
            BigDecimal rate = rates == null ? null : firstPositiveValue(rates, "VES");
            if (rate == null) {
                throw new BcvResponseException("ER-API no publicó el par USD/VES.");
            }

            long updatedAt = response.optLong("time_last_update_unix", 0L) * 1000L;
            if (updatedAt <= 0L) {
                updatedAt = System.currentTimeMillis();
            }
            return new RateQuote(rate, "ER-API (respaldo)", updatedAt, true);
        } catch (org.json.JSONException exception) {
            throw new BcvResponseException("La respuesta JSON de ER-API no es válida.", exception);
        }
    }

    private static BigDecimal firstPositiveValue(
            org.json.JSONObject object,
            String... fieldNames) throws java.io.IOException {
        for (String fieldName : fieldNames) {
            Object rawValue;
            try {
                rawValue = object.opt(fieldName);
            } catch (RuntimeException exception) {
                throw new BcvResponseException("No se pudo leer el campo " + fieldName + ".", exception);
            }
            if (rawValue == null || rawValue == org.json.JSONObject.NULL) {
                continue;
            }
            try {
                BigDecimal value = new BigDecimal(rawValue.toString());
                if (value.signum() > 0) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                throw new BcvResponseException("El campo " + fieldName + " no es un número.", exception);
            }
        }
        return null;
    }

    private static String readHttps(String address, String expectedHost, String accept)
            throws java.io.IOException {
        javax.net.ssl.HttpsURLConnection connection = null;
        java.io.InputStream input = null;
        java.io.Reader reader = null;

        try {
            java.net.URL url = new java.net.URL(address);
            if (!"https".equalsIgnoreCase(url.getProtocol())
                    || !expectedHost.equalsIgnoreCase(url.getHost())) {
                throw new BcvResponseException("Endpoint HTTPS no autorizado.");
            }
            connection = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", accept);
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 21) {
                connection.setSSLSocketFactory(new Tls12SocketFactory(
                        (SSLSocketFactory) SSLSocketFactory.getDefault()));
            }

            int responseCode = connection.getResponseCode();
            java.net.URL finalUrl = connection.getURL();
            if (!"https".equalsIgnoreCase(finalUrl.getProtocol())
                    || !expectedHost.equalsIgnoreCase(finalUrl.getHost())) {
                throw new BcvResponseException("La respuesta salió del host HTTPS permitido.");
            }
            if (responseCode != 200) {
                throw new BcvResponseException(
                        "El proveedor respondió HTTP " + responseCode + ".");
            }

            input = connection.getInputStream();
            reader = new java.io.InputStreamReader(input, "UTF-8");
            return readBounded(reader);
        } finally {
            closeQuietly(reader);
            closeQuietly(input);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBounded(java.io.Reader reader) throws java.io.IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            if (content.length() + count > MAX_RESPONSE_CHARS) {
                throw new java.io.IOException("La página del BCV supera el límite esperado.");
            }
            content.append(buffer, 0, count);
        }
        return content.toString();
    }

    private static BigDecimal parseDollarRate(String html) throws java.io.IOException {
        Matcher sectionMatcher = DOLLAR_SECTION.matcher(html);
        if (!sectionMatcher.find()) {
            throw new java.io.IOException("No se encontró la sección USD del BCV.");
        }

        int sectionStart = sectionMatcher.end();
        String dollarSection = html.substring(sectionStart);
        Matcher usdMatcher = USD_LABEL.matcher(dollarSection);
        if (!usdMatcher.find()) {
            throw new java.io.IOException("La sección del BCV no identifica USD.");
        }

        Matcher rateMatcher = RATE_IN_STRONG.matcher(dollarSection.substring(usdMatcher.end()));
        if (!rateMatcher.find()) {
            throw new java.io.IOException("No se encontró el valor USD publicado por el BCV.");
        }
        String candidate = rateMatcher.group(1).trim();
        if (!NUMBER.matcher(candidate).matches()) {
            throw new java.io.IOException("El valor USD del BCV tiene un formato no válido.");
        }
        BigDecimal rate = parseDecimal(candidate);
        if (rate.signum() <= 0) {
            throw new java.io.IOException("El valor USD del BCV debe ser mayor que cero.");
        }
        return rate;
    }

    private static Long parseIsoTimestamp(String timestamp) {
        if (timestamp == null || timestamp.length() < 19) {
            return null;
        }
        String normalized = timestamp.trim().replaceFirst("([+-][0-9]{2}):([0-9]{2})$", "$1$2");
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1) + "+0000";
        }
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ssZ",
        };
        for (String formatString : formats) {
            SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.US);
            format.setLenient(false);
            java.text.ParsePosition position = new java.text.ParsePosition(0);
            Date parsed = format.parse(normalized, position);
            if (parsed != null && position.getIndex() == normalized.length()) {
                return parsed.getTime();
            }
        }
        return null;
    }

    private static BigDecimal parseDecimal(String raw) throws java.io.IOException {
        String value = raw.replace(" ", "");
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');

        if (comma >= 0 && dot >= 0) {
            if (comma > dot) {
                value = value.replace(".", "").replace(',', '.');
            } else {
                value = value.replace(",", "");
            }
        } else if (comma >= 0) {
            value = value.replace(',', '.');
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new java.io.IOException("El valor USD del BCV no se pudo interpretar.");
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (java.io.IOException ignored) {
        }
    }

    static final class BcvResponseException extends java.io.IOException {
        BcvResponseException(String message) {
            super(message);
        }

        BcvResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static final class RateQuote {
        final BigDecimal rate;
        final String source;
        final long updatedAtMillis;
        final boolean usedFallback;

        RateQuote(BigDecimal rate, String source, long updatedAtMillis, boolean usedFallback) {
            this.rate = rate;
            this.source = source;
            this.updatedAtMillis = updatedAtMillis;
            this.usedFallback = usedFallback;
        }
    }

    private static final class Tls12SocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        Tls12SocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public java.net.Socket createSocket() throws java.io.IOException {
            return enableModernTls(delegate.createSocket());
        }

        @Override
        public java.net.Socket createSocket(
                java.net.Socket socket,
                String host,
                int port,
                boolean autoClose) throws java.io.IOException {
            return enableModernTls(delegate.createSocket(socket, host, port, autoClose));
        }

        @Override
        public java.net.Socket createSocket(String host, int port) throws java.io.IOException {
            return enableModernTls(delegate.createSocket(host, port));
        }

        @Override
        public java.net.Socket createSocket(
                String host,
                int port,
                java.net.InetAddress localHost,
                int localPort) throws java.io.IOException {
            return enableModernTls(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public java.net.Socket createSocket(java.net.InetAddress host, int port)
                throws java.io.IOException {
            return enableModernTls(delegate.createSocket(host, port));
        }

        @Override
        public java.net.Socket createSocket(
                java.net.InetAddress address,
                int port,
                java.net.InetAddress localAddress,
                int localPort) throws java.io.IOException {
            return enableModernTls(delegate.createSocket(address, port, localAddress, localPort));
        }

        private java.net.Socket enableModernTls(java.net.Socket socket) {
            if (!(socket instanceof SSLSocket)) {
                return socket;
            }
            SSLSocket sslSocket = (SSLSocket) socket;
            java.util.ArrayList<String> enabledProtocols = new java.util.ArrayList<String>();
            for (String protocol : sslSocket.getSupportedProtocols()) {
                if ("TLSv1.2".equals(protocol) || "TLSv1.3".equals(protocol)) {
                    enabledProtocols.add(protocol);
                }
            }
            if (!enabledProtocols.isEmpty()) {
                sslSocket.setEnabledProtocols(enabledProtocols.toArray(new String[0]));
            }
            return sslSocket;
        }
    }
}
