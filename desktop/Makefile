cc = g++
ld = g++
cflags = -lm -c
lflags = `pkg-config --cflags --libs opencv` -lm

src = wiperDetect.cpp 
obj = wiperDetect.o
exe = wiperDetect

all: $(exe)

$(exe): $(obj)
	$(ld) $(obj) -o $(exe) $(lflags) 

clean:
	rm -f *.o
	rm -f $(exe)
