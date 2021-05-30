# eclipse-format

This is a commandline java formatter based on Eclipse, but you doesn't have to install the entire Eclipse IDE to use it.

## Building

```sh
mvn package
sudo ./install.sh
```

## Usage

```
usage: eclipse-format [options] [<file> ...]
Standalone Eclipse Formatter

If no files are specified, it reads the code from stdin and write to
stdout. Otherwise it reformats the files.

 -c,--config <arg>   path to config file
 -h,--help           print this help message
```
