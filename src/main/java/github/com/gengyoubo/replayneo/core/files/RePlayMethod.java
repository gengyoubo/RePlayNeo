package github.com.gengyoubo.replayneo.core.files;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

public class RePlayMethod {

    public static File createTempDir() {
        try {
            return java.nio.file.Files
                    .createTempDirectory("resourcepack-")
                    .toFile();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to create temporary directory", e);
        }
    }
    public static String encodeFileName(
            String str) {

        StringBuilder sb = new StringBuilder();

        for (char c : str.toCharArray()) {

            if (Character.isLetterOrDigit(c)
                    || c == '-'
                    || c == '_'
                    || c == ' ') {

                sb.append(c);

            } else {

                byte[] bytes =
                        String.valueOf(c)
                                .getBytes(
                                        StandardCharsets.UTF_8);

                for (byte b : bytes) {
                    sb.append(
                            String.format(
                                    "%%%02X",
                                    b & 0xff));
                }
            }
        }

        return sb.toString();
    }
    public static String getNameWithoutExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index == -1 ? fileName : fileName.substring(0, index);
    }

    public static String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index == -1 ? "" : fileName.substring(index + 1);
    }
}
