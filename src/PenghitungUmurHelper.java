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
                // API: http://history.muffinlabs.com/date/month/day
                String urlString = "http://history.muffinlabs.com/date/" + month + "/" + day;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000); // 8 detik timeout
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                     throw new Exception("HTTP Error Code: " + responseCode + " - Gagal mengambil data.");
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                conn.disconnect();

                if (shouldStop.get()) {
                    javax.swing.SwingUtilities.invokeLater(() -> txtAreaPeristiwa.setText("Pengambilan data dibatalkan.\n"));
                    return;
                }

                // Parsing JSON
                JSONObject json = new JSONObject(content.toString());
                
                // Cek dan navigasi ke objek 'data' lalu array 'Events'
                if (!json.has("data") || !json.getJSONObject("data").has("Events")) {
                    throw new Exception("Struktur JSON tidak sesuai (Missing 'data' or 'Events').");
                }
                
                JSONObject data = json.getJSONObject("data");
                JSONArray events = data.getJSONArray("Events");

                javax.swing.SwingUtilities.invokeLater(() -> 
                    txtAreaPeristiwa.setText("Peristiwa Sejarah (" + month + "/" + day + " - Bahasa Inggris):\n---------------------------------------------------------------\n")
                );

                if (events.length() == 0) {
                    javax.swing.SwingUtilities.invokeLater(() ->
                            txtAreaPeristiwa.append("Tidak ada peristiwa penting yang ditemukan pada tanggal ini."));
                    return;
                }

                for (int i = 0; i < events.length(); i++) {
                    if (shouldStop.get()) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                                txtAreaPeristiwa.append("\nPengambilan data dihentikan.");
                                txtAreaPeristiwa.setCaretPosition(0);
                            });
                        return;
                    }

                    JSONObject event = events.getJSONObject(i);
                    String year = event.getString("year");
                    String description = event.getString("text"); // Menggunakan key 'text'

                    javax.swing.SwingUtilities.invokeLater(() ->
                            txtAreaPeristiwa.append(year + ": " + description + "\n"));
                }
                
                javax.swing.SwingUtilities.invokeLater(() -> txtAreaPeristiwa.setCaretPosition(0));

            } catch (Exception e) {
                String errorMsg = e.getMessage();
                System.err.println("API Error: " + errorMsg); // Log error di console
                javax.swing.SwingUtilities.invokeLater(() -> {
                    txtAreaPeristiwa.setText("❌ Gagal mendapatkan data peristiwa. Kemungkinan:\n" + 
                                            "1. Koneksi internet terputus.\n" + 
                                            "2. Firewall memblokir akses.\n" +
                                            "3. Library 'org.json.jar' belum terpasang.\n" +
                                            "Detail: " + errorMsg);
                });
            }
        }).start();
    }
}