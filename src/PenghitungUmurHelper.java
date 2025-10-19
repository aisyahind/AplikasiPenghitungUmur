import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;
import javax.swing.JTextArea;
// ⚠️ Wajib ada: org.json.jar di library proyek Anda
import org.json.JSONArray;
import org.json.JSONObject;

public class PenghitungUmurHelper {

    // ... (Metode hitungUmurDetail, hariUlangTahunBerikutnya, getDayOfWeekInIndonesian tetap sama) ...

    public String hitungUmurDetail(LocalDate lahir, LocalDate sekarang) {
        Period period = Period.between(lahir, sekarang);
        return period.getYears() + " tahun, " + period.getMonths() + " bulan, " + period.getDays() + " hari";
    }

    public LocalDate hariUlangTahunBerikutnya(LocalDate lahir, LocalDate sekarang) {
        LocalDate ulangTahunBerikutnya = lahir.withYear(sekarang.getYear());
        if (!ulangTahunBerikutnya.isAfter(sekarang)) {
            ulangTahunBerikutnya = ulangTahunBerikutnya.plusYears(1);
        }
        return ulangTahunBerikutnya;
    }

    public String getDayOfWeekInIndonesian(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Senin";
            case TUESDAY -> "Selasa";
            case WEDNESDAY -> "Rabu";
            case THURSDAY -> "Kamis";
            case FRIDAY -> "Jumat";
            case SATURDAY -> "Sabtu";
            case SUNDAY -> "Minggu";
            default -> date.getDayOfWeek().toString();
        };
    }

    // Metode utama untuk mengambil peristiwa
    public void fetchPeristiwaPenting(int month, int day, JTextArea txtAreaPeristiwa, Supplier<Boolean> shouldStop) {
    new Thread(() -> {
        try {
            // Gunakan HTTPS agar tidak kena redirect 308
            String urlString = "https://history.muffinlabs.com/date/" + month + "/" + day;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true); // otomatis ikuti redirect
            conn.setConnectTimeout(10000); // 10 detik
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("HTTP Response: " + responseCode);
                throw new Exception("HTTP Error Code: " + responseCode + " - Gagal mengambil data dari server.");
            }

            // Baca hasil respon
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            if (shouldStop.get()) {
                javax.swing.SwingUtilities.invokeLater(() ->
                        txtAreaPeristiwa.setText("Pengambilan data dibatalkan.\n"));
                return;
            }

            // Parsing JSON
            String jsonText = content.toString().trim();
            if (!jsonText.startsWith("{")) {
                throw new Exception("Response bukan JSON valid: " + jsonText.substring(0, Math.min(200, jsonText.length())));
            }

            JSONObject json = new JSONObject(jsonText);
            if (!json.has("data") || !json.getJSONObject("data").has("Events")) {
                throw new Exception("Struktur JSON tidak sesuai (Missing 'data' or 'Events').");
            }

            JSONObject data = json.getJSONObject("data");
            JSONArray events = data.getJSONArray("Events");

            javax.swing.SwingUtilities.invokeLater(() ->
                    txtAreaPeristiwa.setText("📜 Peristiwa Sejarah (" + day + "/" + month + "):\n------------------------------------------\n"));

            if (events.length() == 0) {
                javax.swing.SwingUtilities.invokeLater(() ->
                        txtAreaPeristiwa.append("Tidak ada peristiwa penting yang ditemukan pada tanggal ini."));
                return;
            }

            for (int i = 0; i < events.length(); i++) {
                if (shouldStop.get()) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        txtAreaPeristiwa.append("\n⛔ Pengambilan data dihentikan.");
                        txtAreaPeristiwa.setCaretPosition(0);
                    });
                    return;
                }

                JSONObject event = events.getJSONObject(i);
                String year = event.optString("year", "Tahun tidak diketahui");
                String description = event.optString("text", "(Tanpa deskripsi)");

                // 🔹 Translasi otomatis ke Bahasa Indonesia
                String translated = translateToIndonesian(description);

                javax.swing.SwingUtilities.invokeLater(() ->
                        txtAreaPeristiwa.append(year + ": " + translated + "\n"));
            }

            javax.swing.SwingUtilities.invokeLater(() ->
                    txtAreaPeristiwa.setCaretPosition(0));

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            System.err.println("API Error: " + errorMsg);
            javax.swing.SwingUtilities.invokeLater(() -> {
                txtAreaPeristiwa.setText("❌ Gagal mendapatkan data peristiwa. Kemungkinan:\n" +
                        "1. Koneksi internet terputus.\n" +
                        "2. Server API sedang dialihkan atau down.\n" +
                        "3. Library org.json.jar belum terpasang.\n\n" +
                        "Detail: " + errorMsg);
            });
        }
    }).start();
}
    
    private String translateToIndonesian(String text) {
    try {
        String apiUrl = "https://api.mymemory.translated.net/get?q=" +
                java.net.URLEncoder.encode(text, "UTF-8") +
                "&langpair=en|id";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();

        JSONObject json = new JSONObject(response.toString());
        if (json.has("responseData")) {
            return json.getJSONObject("responseData").optString("translatedText", text);
        }
    } catch (Exception e) {
        System.err.println("Gagal menerjemahkan: " + e.getMessage());
    }
    return text; // fallback jika gagal translate
}
}