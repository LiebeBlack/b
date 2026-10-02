package com.liebeblack.divtrack.lite;

import android.app.Activity;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
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

public final class MainActivity extends Activity {
    private static final String PREFERENCES = "lite_rate_cache";
    private static final String KEY_RATE = "official_rate";
    private static final String KEY_CHECKED_AT = "checked_at";

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

        rateValue.setText(formatRate(new BigDecimal(cachedRate)));
        fitRateTextToWidth();
        if (checkedAtMillis > 0L) {
            checkedAt.setText(getString(R.string.checked_at, formatTimestamp(checkedAtMillis)));
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

        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final BigDecimal fetchedRate;
                try {
                    fetchedRate = BcvRateClient.fetchOfficialUsdRate();
                } catch (Exception exception) {
                    Log.w("DivTrackLite", "No se pudo actualizar la tasa BCV.", exception);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!activityDestroyed) {
                                finishRefreshWithError();
                            }
                        }
                    });
                    return;
                }

                final long fetchedAtMillis = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activityDestroyed) {
                            return;
                        }
                        getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_RATE, fetchedRate.toPlainString())
                                .putLong(KEY_CHECKED_AT, fetchedAtMillis)
                                .apply();
                        rateValue.setText(formatRate(fetchedRate));
                        checkedAt.setText(getString(
                                R.string.checked_at,
                                formatTimestamp(fetchedAtMillis)));
                        status.setText(R.string.status_updated);
                        fitRateTextToWidth();
                        finishRefresh();
                    }
                });
            }
        });
    }

    private void finishRefreshWithError() {
        if (isFinishing()) {
            return;
        }
        boolean hasCachedRate = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .contains(KEY_RATE);
        status.setText(hasCachedRate
                ? R.string.status_update_failed
                : R.string.status_empty_failed);
        finishRefresh();
    }

    private void finishRefresh() {
        isRefreshing = false;
        refreshButton.setEnabled(true);
        refreshButton.setText(R.string.refresh);
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
    private static final int CONNECT_TIMEOUT_MS = 12000;
    private static final int READ_TIMEOUT_MS = 12000;
    private static final int MAX_RESPONSE_CHARS = 1_500_000;

    private static final Pattern DOLLAR_BLOCK = Pattern.compile(
            "(?is)<[^>]+\\bid\\s*=\\s*['\"]dolar['\"][^>]*>(.*?)"
                    + "(?=<[^>]+\\bid\\s*=\\s*['\"](?:euro|yuan|lira)['\"]|$)");
    private static final Pattern RATE_IN_STRONG = Pattern.compile(
            "(?is)<strong[^>]*>\\s*([0-9][0-9.,\\s]*)\\s*</strong>");
    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9.,]*");

    private BcvRateClient() {
    }

    static BigDecimal fetchOfficialUsdRate() throws Exception {
        javax.net.ssl.HttpsURLConnection connection = null;
        java.io.InputStream input = null;
        java.io.Reader reader = null;

        try {
            java.net.URL url = new java.net.URL(BCV_URL);
            connection = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "text/html");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("User-Agent", "DivTrackLite/1.0 (Android)");

            int responseCode = connection.getResponseCode();
            java.net.URL finalUrl = connection.getURL();
            if (responseCode != 200
                    || !"https".equalsIgnoreCase(finalUrl.getProtocol())
                    || !isBcvHost(finalUrl.getHost())) {
                throw new java.io.IOException("Respuesta no válida del sitio oficial.");
            }

            input = connection.getInputStream();
            reader = new java.io.InputStreamReader(input, "UTF-8");
            String html = readBounded(reader);
            return parseDollarRate(html);
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
        Matcher blockMatcher = DOLLAR_BLOCK.matcher(html);
        if (!blockMatcher.find()) {
            throw new java.io.IOException("No se encontró la sección USD del BCV.");
        }

        Matcher rateMatcher = RATE_IN_STRONG.matcher(blockMatcher.group(1));
        while (rateMatcher.find()) {
            String candidate = rateMatcher.group(1).trim();
            if (!NUMBER.matcher(candidate).matches()) {
                continue;
            }
            BigDecimal rate = parseDecimal(candidate);
            if (rate.signum() > 0) {
                return rate;
            }
        }
        throw new java.io.IOException("El valor USD del BCV no tiene un formato reconocido.");
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

    private static boolean isBcvHost(String host) {
        return "bcv.org.ve".equalsIgnoreCase(host)
                || "www.bcv.org.ve".equalsIgnoreCase(host);
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
}
