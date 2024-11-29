package org.eytril.spigot.util;

public class JavaUtil {
    public static int getJavaVersionOpcode() {
        String javaVersion = System.getProperty("java.version");
        int majorVersion;

        // Parse the version string
        if (javaVersion.startsWith("1.")) {
            majorVersion = Integer.parseInt(javaVersion.substring(2, 3)); // e.g., "1.8" -> 8
        } else {
            int dotIndex = javaVersion.indexOf('.');
            majorVersion = dotIndex == -1
                    ? Integer.parseInt(javaVersion) // e.g., "11"
                    : Integer.parseInt(javaVersion.substring(0, dotIndex)); // e.g., "17.0.1"
        }

        // Return the corresponding opcode version -- Update for new versions.
        switch (majorVersion) {
            case 8:  return 52;
            case 9:  return 53;
            case 10: return 54;
            case 11: return 55;
            case 12: return 56;
            case 13: return 57;
            case 14: return 58;
            case 15: return 59;
            case 16: return 60;
            case 17: return 61;
            case 18: return 62;
            case 19: return 63;
            case 20: return 64;
            default: throw new UnsupportedOperationException("Unsupported Java version: " + majorVersion);
        }
    }
}
