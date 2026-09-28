import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CentralinaIoT {

    static class LetturaInvalidaException extends IllegalArgumentException {
        public LetturaInvalidaException(String messaggio) {
            super(messaggio);
        }
    }

    static class LetturaSensore {
        Double temperatura;
        Integer umiditaPercentuale;
        Long timestampUnix;
        Boolean batteriaScarica;

        @Override
        public String toString() {
            return "Lettura[temp=" + temperatura + ", umid=" + umiditaPercentuale
                    + ", ts=" + timestampUnix + ", battLow=" + batteriaScarica + "]";
        }
    }

    static int campiCorrotti = 0;

    static Optional<LetturaSensore> parsePacchetto(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Optional.empty();
        }

        LetturaSensore l = new LetturaSensore();
        boolean almenoUnCampo = false;

        for (String coppia : raw.split(";")) {
            String[] kv = coppia.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String chiave = kv[0].trim();
            String valore = kv[1].trim();

            try {
                switch (chiave) {
                    case "temp":
                        l.temperatura = Double.valueOf(valore);
                        almenoUnCampo = true;
                        break;

                    case "umid":
                        Integer u = Integer.valueOf(valore);
                        if (u < 0 || u > 100) {
                            throw new LetturaInvalidaException(
                                    "Umidita' fuori range (0-100): " + u);
                        }
                        l.umiditaPercentuale = u;
                        almenoUnCampo = true;
                        break;

                    case "ts":
                        l.timestampUnix = Long.valueOf(valore);
                        almenoUnCampo = true;
                        break;

                    case "batt_low":
                        if (valore.equalsIgnoreCase("true")) {
                            l.batteriaScarica = Boolean.TRUE;
                        } else if (valore.equalsIgnoreCase("false")) {
                            l.batteriaScarica = Boolean.FALSE;
                        } else {
                            throw new NumberFormatException("Booleano non valido: " + valore);
                        }
                        almenoUnCampo = true;
                        break;

                    default:
                        break;
                }
            } catch (NumberFormatException e) {
                campiCorrotti++;
                System.err.println("[LOG] Campo corrotto '" + chiave + "=" + valore
                        + "' -> " + e.getMessage());
            }
        }

        return almenoUnCampo ? Optional.of(l) : Optional.empty();
    }

    static boolean confrontaBatteriaSbagliato(Integer a, Integer b) {
        return a == b;
    }

    static boolean confrontaBatteria(Integer a, Integer b) {
        return Objects.equals(a, b);
    }

    static void testAutoboxing() {
        Integer a1 = 100, b1 = 100;
        Integer a2 = 1000, b2 = 1000;

        System.out.println("=== Test Integer Cache ===");
        System.out.println("100  == 100  (sbagliato): " + confrontaBatteriaSbagliato(a1, b1));
        System.out.println("1000 == 1000 (sbagliato): " + confrontaBatteriaSbagliato(a2, b2));
        System.out.println("100  equals 100  (corretto): " + confrontaBatteria(a1, b1));
        System.out.println("1000 equals 1000 (corretto): " + confrontaBatteria(a2, b2));
        System.out.println();
    }

    static void ordinaPerTemperaturaDecrescente(List<LetturaSensore> lista) {
        lista.sort((x, y) -> {
            if (x.temperatura == null && y.temperatura == null) return 0;
            if (x.temperatura == null) return 1;
            if (y.temperatura == null) return -1;
            return Double.compare(y.temperatura, x.temperatura);
        });
    }

    static void ordinaPerUmiditaPoiBatteria(List<LetturaSensore> lista) {
        lista.sort((x, y) -> {
            int u1 = x.umiditaPercentuale == null ? -1 : x.umiditaPercentuale;
            int u2 = y.umiditaPercentuale == null ? -1 : y.umiditaPercentuale;
            int c = Integer.compare(u1, u2);
            if (c != 0) return c;
            return Boolean.compare(Boolean.TRUE.equals(x.batteriaScarica),
                    Boolean.TRUE.equals(y.batteriaScarica));
        });
    }

    public static void main(String[] args) {
        testAutoboxing();

        String[] pacchetti = {
                "temp=23.5;umid=61;ts=1732000000;batt_low=false",
                "temp=19.0;umid=45;ts=1732000060;batt_low=true",
                "temp=xx.xx;umid=50;ts=1732000120;batt_low=false",
                "umid=70;ts=1732000180",
                "temp=31.2;umid=150;ts=1732000240;batt_low=false",
                "",
                "temp=-5.5;ts=1732000300;batt_low=maybe",
                "temp=27.8;umid=55;ts=1732000360;batt_low=false",
                "garbage!!!",
                "batt_low=true"
        };

        List<LetturaSensore> valide = new ArrayList<>();
        int errori = 0;

        for (String p : pacchetti) {
            System.out.println("Pacchetto: \"" + p + "\"");
            try {
                Optional<LetturaSensore> res = parsePacchetto(p);
                if (res.isPresent()) {
                    valide.add(res.get());
                    System.out.println("  -> OK " + res.get());
                } else {
                    errori++;
                    System.out.println("  -> ERRORE: pacchetto vuoto o illeggibile");
                }
            } catch (LetturaInvalidaException e) {
                errori++;
                System.out.println("  -> ERRORE: " + e.getMessage());
            }
        }

        double somma = 0;
        int conTemp = 0;
        for (LetturaSensore l : valide) {
            if (l.temperatura != null) {
                somma += l.temperatura;
                conTemp++;
            }
        }

        System.out.println("\n===== REPORT =====");
        System.out.println("Letture valide      : " + valide.size());
        System.out.println("Errori di parsing   : " + errori);
        System.out.println("Campi corrotti      : " + campiCorrotti);
        if (conTemp > 0) {
            System.out.printf("Temperatura media   : %.2f (su %d letture con temperatura)%n",
                    somma / conTemp, conTemp);
        } else {
            System.out.println("Temperatura media   : n/d");
        }

        ordinaPerTemperaturaDecrescente(valide);
        System.out.println("\nLetture per temperatura decrescente (null in fondo):");
        for (LetturaSensore l : valide) {
            System.out.println("  " + l);
        }
    }
}