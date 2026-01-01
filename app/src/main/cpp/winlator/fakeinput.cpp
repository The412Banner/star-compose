
#include <algorithm>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <map>
#include <mutex>
#include <string>
#include <vector>

#include <dirent.h>
#include <dlfcn.h>

#include <fcntl.h>
#include <linux/input.h>
#include <signal.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <sys/inotify.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>

//Thanks pipetto-crypto for controller cope

#define EXPORT __attribute__((visibility("default")))

static std::unordered_map<int, std::string> controller_map;
static std::mutex map_mutex;
static bool initialized = false;
volatile sig_atomic_t stop_flag = 0;

static const char *get_fake_input_path() {
  static const char *path = nullptr;
  if (!path) {
    path = getenv("FAKEINPUT_PATH");
    if (!path)
      path = "/data/data/com.winlator.cmod/files/imagefs/dev/input";
  }
  return path;
}

void handle_sigint(int sig)  { 
    stop_flag = 1;
} 

void setup_signal_handler() {
    if (!initialized) {
        signal(SIGINT, handle_sigint);
        initialized = true;
    }
}

__attribute__((visibility("hidden"))) char *
from_real_to_fake_path(const char *pathname) {
  if (!pathname)
    return nullptr;
  const char *event = strrchr(pathname, '/') + 1;
  char *fake_path;
  asprintf(&fake_path, "%s/%s", get_fake_input_path(), event);
  return fake_path;
}

EXPORT
extern "C" int open(const char *pathname, int flags, ...) {
  va_list va;
  mode_t mode;
  int fd;
  bool hasMode;
  bool isFromInput;
  int (*my_open)(const char *, int, ...);

  va_start(va, flags);

  hasMode = flags & O_CREAT;
  isFromInput = false;

  if (hasMode) {
    mode = va_arg(va, mode_t);
  }

  va_end(va);

  *(void **)&my_open = dlsym(RTLD_NEXT, "open");

  if (pathname) {
    if (strstr(pathname, "/dev/input/event")) {
      pathname = from_real_to_fake_path(pathname);
      isFromInput = true;
    } else if (!strcmp(pathname, "/dev/input")) {
      pathname = get_fake_input_path();
    }
  }

  if (hasMode)
    fd = my_open(pathname, flags, mode);
  else
    fd = my_open(pathname, flags);

  if (isFromInput)
    controller_map[fd] = pathname;

  return fd;
}

EXPORT
extern "C" int openat(int dirfd, const char *pathname, int flags, ...) {
  va_list va;
  mode_t mode;
  int fd;
  bool hasMode;
  bool isFromInput;
  int (*my_openat)(int, const char *, int, ...);

  va_start(va, flags);

  isFromInput = false;
  hasMode = flags & O_CREAT;

  if (hasMode) {
    mode = va_arg(va, mode_t);
  }

  va_end(va);

  *(void **)&my_openat = dlsym(RTLD_NEXT, "openat");

  if (pathname) {
    if (strstr(pathname, "/dev/input/event")) {
      pathname = from_real_to_fake_path(pathname);
      isFromInput = true;
    } else if (!strcmp(pathname, "/dev/input")) {
      pathname = get_fake_input_path();
    }
  }

  if (hasMode)
    fd = my_openat(dirfd, pathname, flags, mode);
  else
    fd = my_openat(dirfd, pathname, flags);

  if (isFromInput)
    controller_map[fd] = pathname;

  return fd;
}

EXPORT
extern "C" int stat(const char *pathname, struct stat *statbuf) {
  int (*my_stat)(const char *, struct stat *);

  *(void **)&my_stat = dlsym(RTLD_NEXT, "stat");

  if (pathname) {
    if (strstr(pathname, "/dev/input/event")) {
      pathname = from_real_to_fake_path(pathname);
    } else if (!strcmp(pathname, "/dev/input")) {
      pathname = get_fake_input_path();
    }
  }

  return my_stat(pathname, statbuf);
}

EXPORT
extern "C" int scandir(const char *dirp, struct dirent ***namelist,
                       int (*filter)(const struct dirent *),
                       int (*compar)(const struct dirent **,
                                     const struct dirent **)) {
  int (*my_scandir)(const char *, struct dirent ***,
                    int (*)(const struct dirent *),
                    int (*)(const struct dirent **, const struct dirent **));

  *(void **)&my_scandir = dlsym(RTLD_NEXT, "scandir");

  if (dirp) {
    if (!strcmp(dirp, "/dev/input")) {
      dirp = get_fake_input_path();
    }
  }

  return my_scandir(dirp, namelist, filter, compar);
}

EXPORT
extern "C" int inotify_add_watch(int fd, const char *pathname, uint32_t mask) {
  int (*my_inotify_add_watch)(int, const char *, uint32_t);

  *(void **)&my_inotify_add_watch = dlsym(RTLD_NEXT, "inotify_add_watch");

  if (pathname) {
    if (strstr(pathname, "/dev/input/event")) {
      pathname = from_real_to_fake_path(pathname);
    } else if (!strcmp(pathname, "/dev/input")) {
      pathname = get_fake_input_path();
    }
  }

  return my_inotify_add_watch(fd, pathname, mask);
}

EXPORT
extern "C" int ioctl(int fd, int op, ...) {
  va_list va;
  int (*my_ioctl)(int, int, ...);
  void *argp;

  va_start(va, op);
  argp = va_arg(va, void *);
  va_end(va);

  *(void **)&my_ioctl = dlsym(RTLD_NEXT, "ioctl");

  auto controller = controller_map.find(fd);
  if (controller == controller_map.end()) {
    return syscall(SYS_ioctl, fd, op, argp);
  }

  int type = (op >> 8 & 0xFF);
  int number = (op >> 0 & 0xFF);

  if (type == 0x45 && number == 0x1) {
    printf("Hooking ioctl EVIOCGVERSION\n");
    int version = 65536;
    memcpy(argp, (void *)&version, sizeof(int));
    return 0;
  } else if (type == 0x45 && number == 0x2) {
    printf("Hooking ioctl EVIOCGID\n");
    struct input_id id;
    memset(&id, 0, sizeof(id));
    id.bustype = 0x0;
    id.vendor = 0x0;
    id.product = 0x0;
    id.version = 0x0;
    memcpy(argp, (void *)&id, sizeof(id));
    return 0;
  } else if (type == 0x45 && number == 0x6) {
    printf("Hooking ioctl EVIOCGNAME\n");
    const char *name = "Xbox 360 Controller";
    memcpy(argp, name, strlen(name) + 1);
    return 0;
  } else if (type == 0x45 && number == 0x9) {
    printf("Hooking ioctl EVIOCGPROP\n");
    return 0;
  } else if (type == 0x45 && number == 0x18) {
    printf("Hooking ioctl EVIOCGKEY(len)\n");
    char bitmask[KEY_MAX / 8] = {0};
    memcpy(argp, (void *)&bitmask, sizeof(bitmask));
    return 0;
  } else if (type == 0x45 && number == 0x20) {
    printf("Hooking ioctl EVIOCGBIT(0, len)\n");
    char bitmask[EV_MAX / 8] = {0};
    bitmask[EV_SYN / 8] |= (1 << (EV_SYN % 8));
    bitmask[EV_KEY / 8] |= (1 << (EV_KEY % 8));
    bitmask[EV_ABS / 8] |= (1 << (EV_ABS % 8));
    memcpy(argp, (void *)&bitmask, sizeof(bitmask));
    return 0;
  } else if (type == 0x45 && number == 0x21) {
    printf("Hooking ioctl EVIOCGBIT(EV_KEY, len)\n");
    char bitmask[KEY_MAX / 8] = {0};
    for (int i = 0x130; i <= 0x13e; i++) {
      if (i == 0x130)
        bitmask[BTN_A / 8] |= (1 << (BTN_A % 8));
      else if (i == 0x131)
        bitmask[BTN_B / 8] |= (1 << (BTN_B % 8));
      else if (i == 0x132)
        continue;
      else if (i == 0x133)
        bitmask[BTN_X / 8] |= (1 << (BTN_X % 8));
      else if (i == 0x134)
        bitmask[BTN_Y / 8] |= (1 << (BTN_Y % 8));
      else if (i == 0x135)
        continue;
      else
        bitmask[i / 8] |= (1 << (i % 8));
    }
    memcpy(argp, (void *)&bitmask, sizeof(bitmask));
    return 0;
  } else if (type == 0x45 && number == 0x22) {
    printf("Hooking ioctl EVIOCGBIT(EV_REL, len)\n");
    char bitmask[REL_MAX / 8] = {0};
    memcpy(argp, (void *)&bitmask, sizeof(bitmask));
    return 0;
  } else if (type == 0x45 && number == 0x23) {
    printf("Hooking ioctl EVIOCGBIT(EV_ABS, len)\n");
    char bitmask[ABS_MAX / 8] = {0};
    bitmask[ABS_X / 8] |= (1 << (ABS_X % 8));
    bitmask[ABS_Y / 8] |= (1 << (ABS_Y % 8));
    bitmask[ABS_RX / 8] |= (1 << (ABS_RX % 8));
    bitmask[ABS_RY / 8] |= (1 << (ABS_RY % 8));
    bitmask[ABS_GAS / 8] |= (1 << (ABS_GAS % 8));
    bitmask[ABS_BRAKE / 8] |= (1 << (ABS_BRAKE % 8));
    bitmask[ABS_HAT0X / 8] |= (1 << (ABS_HAT0X % 8));
    bitmask[ABS_HAT0Y / 8] |= (1 << (ABS_HAT0Y % 8));
    memcpy(argp, (void *)&bitmask, sizeof(bitmask));
    return 0;
  } else if (type == 0x45 && number == 0x35) {
    printf("Hooking ioctl EVIOCGBIT(EV_FF, len)\n");
    char bitmask[FF_MAX / 8] = {0};
    bitmask[FF_RUMBLE / 8] |= 0;
    bitmask[FF_SINE / 8] |= 0;
    return 0;
  } else if (type == 0x45 && number >= 0x40 && number <= 0x51) {
    printf("Hooking ioctl EVIOCGABS(ABS)\n");
    struct input_absinfo abs_info;
    memset(&abs_info, 0, sizeof(abs_info));
    if (number >= 0x40 && number <= 0x44) {
      abs_info.value = 0;
      abs_info.minimum = -32768;
      abs_info.maximum = 32767;
    } else if (number >= 0x49 && number <= 0x4A) {
      abs_info.value = 0;
      abs_info.minimum = 0;
      abs_info.maximum = 255;
    } else if (number >= 0x50 && number <= 0x51) {
      abs_info.value = 0;
      abs_info.minimum = -1;
      abs_info.maximum = 1;
    }
    memcpy(argp, (void *)&abs_info, sizeof(abs_info));
    return 0;
  } else if (type == 0x45 && number == 0x90) {
    printf("Hooking ioctl EVIOCGRAB\n");
    /* Maybe find a better way to mimic what EVIOCGRAB does */
    if (argp)
      fopen("/data/data/com.termux/files/home/fake-input/event0", "w+");
    return 0;
  } else if (type == 0x6A && number == 0x13) {
    printf("Hooking ioctl JSIOCGNAME(len)\n");
    const char *name = "Xbox 360 Controller";
    memcpy(argp, (void *)&name, strlen(name) + 1);
    return 0;
  } else {
    printf("Unhandled evdev ioctl, type %d number %d\n", type, number);
    return syscall(SYS_ioctl, fd, op, argp);
  }
}

EXPORT
extern "C" int close(int fd) {
  int (*my_close)(int);

  *(void **)&my_close = dlsym(RTLD_NEXT, "close");

  auto controller = controller_map.find(fd);
  if (controller != controller_map.end()) {
    controller_map.erase(fd);
  }

  return my_close(fd);
}

EXPORT
extern "C" ssize_t read(int fd, void *buf, size_t count) {
  auto controller = controller_map.find(fd);

  if (controller != controller_map.end()) {
    ssize_t bytes_read = 0;
    int flags = fcntl(fd, F_GETFL);
    bool isNonBlock = flags & O_NONBLOCK;
    bytes_read = syscall(SYS_read, fd, buf, count);
    while (bytes_read == 0 && !isNonBlock) {
      setup_signal_handler();
      if (stop_flag) {
        bytes_read = -1;
        errno = EINTR;
        return bytes_read;
      }
      bytes_read = syscall(SYS_read, fd, buf, count);
      continue;
    }

    return bytes_read;
  }
  return syscall(SYS_read, fd, buf, count);
}
