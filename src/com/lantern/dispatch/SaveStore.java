package com.lantern.dispatch;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;

/** AtomicFile retains the preceding valid file if a write is interrupted. */
final class SaveStore {
  private final AtomicFile file;

  SaveStore(Context context) {
    file = new AtomicFile(new File(context.getFilesDir(), "native-campaign-v1.sav"));
  }

  synchronized Game load() throws IOException {
    return SaveCodec.decode(file.readFully());
  }

  synchronized void write(byte[] bytes) throws IOException {
    FileOutputStream stream = null;
    try {
      stream = file.startWrite();
      stream.write(bytes);
      stream.flush();
      stream.getFD().sync();
      file.finishWrite(stream);
    } catch (IOException e) {
      if (stream != null) file.failWrite(stream);
      throw e;
    }
  }

  boolean exists() {
    return file.getBaseFile().exists() || new File(file.getBaseFile() + ".bak").exists();
  }
}
