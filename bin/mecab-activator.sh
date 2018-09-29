#!/bin/bash

# use this repository files: https://github.com/kazuhira-r/kuromoji-with-mecab-neologd-buildscript
# do not use this script during the period of using libMeCab.so (this script contains install of it.)
########## Init ##########
SCRIPT_NAME=$0
NEOLOGD_BUILD_WORK_DIR=`pwd`

########## Proxy Settings ##########
#export http_proxy=http://your.proxy-host:your.proxy-port
#export https_proxy=http://your.proxy-host:your.proxy-port
#export ANT_OPTS='-DproxyHost=your.proxy-host -DproxyPort=your.proxy-port'

########## Define Functions ##########
logging() {
    LABEL=$1
    LEVEL=$2
    MESSAGE=$3

    TIME=`date +"%Y-%m-%d %H:%M:%S"`

    echo "### [$TIME] [$LABEL] [$LEVEL] $MESSAGE"
}

usage() {
    cat <<EOF
Usage: ${SCRIPT_NAME} [options...]
  options:
    -h ... print this help.
EOF
}

########## Default & Fixed Values ##########
## MeCab
MECAB_VERSION=mecab-0.996
MECAB_INSTALL_DIR=${NEOLOGD_BUILD_WORK_DIR}/mecab

## mecab-ipadic-NEologd Target Tag
DEFAULT_MECAB_IPADIC_NEOLOGD_TAG=master
MECAB_IPADIC_NEOLOGD_TAG=${DEFAULT_MECAB_IPADIC_NEOLOGD_TAG}

## install adjective ext
DEFAULT_INSTALL_ADJECTIVE_EXT=0
INSTALL_ADJECTIVE_EXT=${DEFAULT_INSTALL_ADJECTIVE_EXT}

########## Arguments Process ##########

shift `expr "${OPTIND}" - 1`

logging main INFO 'START.'

cat <<EOF

####################################################################
applied build options.

[Auto Install MeCab Version                  ]    ... ${MECAB_VERSION}
[mecab-ipadic-NEologd Tag                (-N)]    ... ${MECAB_IPADIC_NEOLOGD_TAG}

####################################################################

EOF

sleep 3

if [ ! `which mecab` ]; then
    if [ ! -e ${MECAB_INSTALL_DIR}/bin/mecab ]; then
        logging mecab INFO 'MeCab Install Local.'

        if [ ! -e ${MECAB_VERSION}.tar.gz ]; then
            curl 'https://drive.google.com/uc?export=download&id=0B4y35FiV1wh7cENtOXlicTFaRUE' -L -o ${MECAB_VERSION}.tar.gz
        fi
        tar -zxf ${MECAB_VERSION}.tar.gz
        cd ${MECAB_VERSION}

        if [ ! -e ${MECAB_INSTALL_DIR} ]; then
            mkdir -p ${MECAB_INSTALL_DIR}
        fi

        ./configure --prefix=${MECAB_INSTALL_DIR} --with-charset=utf8
        make
        sudo make install
    fi

    PATH=${MECAB_INSTALL_DIR}/bin:${PATH}
fi
cd ${NEOLOGD_BUILD_WORK_DIR}/build

wget --no-check-certificate 'https://drive.google.com/uc?export=download&id=0B4y35FiV1wh7MWVlSDBCSXZMTXM' -O mecab-ipadic-2.7.0-20070801.tar.gz
tar zxfv mecab-ipadic-2.7.0-20070801.tar.gz
cd mecab-ipadic-2.7.0-20070801
./configure  --with-charset=utf8
make
sudo make install

cd ${NEOLOGD_BUILD_WORK_DIR}

logging mecab-ipadic-NEologd INFO 'Download mecab-ipadic-NEologd.'
if [ ! -e mecab-ipadic-neologd ]; then
    git clone --depth 1 https://github.com/neologd/mecab-ipadic-neologd.git
else
    cd mecab-ipadic-neologd

    if [ -d build ]; then
        rm -rf build
    fi

    git checkout master
    git fetch origin
    git reset --hard origin/master
    git pull --tags
    cd ..
fi

cd mecab-ipadic-neologd

git checkout ${MECAB_IPADIC_NEOLOGD_TAG}

if [ $? -ne 0 ]; then
    logging mecab-ipadic-NEologd ERROR "git checkout[${MECAB_IPADIC_NEOLOGD_TAG}] failed. Please re-run after execute 'rm -f mecab-ipadic-neologd'"
    exit 1
fi


DIR=`pwd`

NEOLOGD_BUILD_DIR=`find ${DIR}/build/mecab-ipadic-* -maxdepth 1 -type d`
NEOLOGD_DIRNAME=`basename ${NEOLOGD_BUILD_DIR}`
NEOLOGD_VERSION_DATE=`echo ${NEOLOGD_DIRNAME} | perl -wp -e 's!.+-(\d+)!$1!'`
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
wget http://pkgs.fedoraproject.org/repo/pkgs/mecab-java/mecab-java-0.996.tar.gz/e50066ae2458a47b5fdc7e119ccd9fdd/mecab-java-0.996.tar.gz
tar vzxf mecab-java-0.996.tar.gz
cd mecab-java-0.996
cp ${NEOLOGD_BUILD_WORK_DIR}/mkfile/Makefile.mecab.java.mk Makefile
sudo make
sudo install ../../mecab/lib/libmecab.so ../../mecab/lib/libmecab.so.2 ../../mecab/lib/libmecab.so.2.0.0 /usr/lib/hadoop/lib/native/
cp MeCab.jar ${NEOLOGD_BUILD_WORK_DIR}/..
mkdir ${NEOLOGD_BUILD_WORK_DIR}/lib
cp MeCab.jar ${NEOLOGD_BUILD_WORK_DIR}/lib
export LD_LIBRARY_PATH=/usr/lib64:/usr/lib/hadoop-lzo/lib/native:/usr/lib/hadoop/lib/native:.
make test

logging main INFO 'END.'
