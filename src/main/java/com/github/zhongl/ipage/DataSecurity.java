package com.github.zhongl.ipage;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@NotThreadSafe
class DataSecurity {

    private static final String SAFE = ".safe";
    private final File dir;

    public DataSecurity(File dir) {
        checkArgument(dir.exists() && dir.isDirectory(), "%s should be an existed directory", dir);
        this.dir = dir;
    }

    /**
     * Validate a file named ".safe" is existed in the dir.
     * If it exists then delete it, or else throw a {@link IllegalStateException} to indicate the data files in the dir
     * should be recovered.
     */
    public void validate() throws UnsafeDataStateException {
        File file = new File(dir, SAFE);
        if (!file.exists()) throw new UnsafeDataStateException();
        checkState(file.delete(), "Can't not remove " + SAFE);
    }


    public void safeClose() throws IOException {
        new File(dir, SAFE).createNewFile();
    }
}