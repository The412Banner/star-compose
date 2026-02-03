file(MAKE_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/epoxy")
file(MAKE_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/virglrenderer")
file(CONFIGURE
        OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/epoxy/config.h"
        CONTENT "\
#pragma once
#define ENABLE_EGL 1
#define EPOXY_PUBLIC __attribute__((visibility(\"default\"))) extern
#define HAVE_KHRPLATFORM_H
")

add_library(epoxy STATIC
        "libepoxy/src/dispatch_common.c"
        "libepoxy/src/dispatch_egl.c"
        "libepoxy/epoxy/gl_generated_dispatch.c"
        "libepoxy/epoxy/gl_generated.h"
        "libepoxy/epoxy/egl_generated_dispatch.c"
        "libepoxy/epoxy/egl_generated.h"
        )
target_include_directories(epoxy PRIVATE "${CMAKE_CURRENT_BINARY_DIR}"
        "${CMAKE_CURRENT_BINARY_DIR}/epoxy"
        "libepoxy"
        "libepoxy/src"
        "libepoxy/include"
        "libepoxy/epoxy"
        )
