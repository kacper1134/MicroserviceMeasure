package utility;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class ClassDecompiler {
    private static final Logger LOG = LogManager.getLogger(ClassDecompiler.class);

    public static String decompile(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            LOG.error("File does not exist: " + filePath);
            return null;
        }

        DecompilerSettings settings = new DecompilerSettings();
        settings.setUnicodeOutputEnabled(true);

        PlainTextOutput output = new PlainTextOutput();
        Decompiler.decompile(filePath, output, settings);

        return output.toString();
    }
}
