package cc.quarkus.c_native.posix;

import static cc.quarkus.qcc.runtime.api.CNative.*;

import cc.quarkus.qcc.runtime.api.Build;

/**
 *
 */
@include(value = "<sys/types.h>", when = Build.Target.IsPosix.class)
public final class SysTypes {

    public static final String LARGEFILE64_SOURCE = "_LARGEFILE64_SOURCE";

    public static final class ssize_t extends word {
    }

    public static final class pid_t extends word {
    }

    public static final class off_t extends word {
    }

    @define(LARGEFILE64_SOURCE)
    public static final class off64_t extends word {
    }

    public static final class loff_t extends word {
    }

    public static final class uid_t extends word {
    }

    public static final class gid_t extends word {
    }

    public static final class mode_t extends word {
    }

}
