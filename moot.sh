#!/bin/bash
set -e

PROPERTIES_FILE=moot.properties
LOCAL_REPO=~/.m2/moot
DEFAULT_MAVEN_MIRROR="http://repo1.maven.org/maven2/org/apache/maven/apache-maven"
DEFAULT_JDK_MIRROR="https://github.com/AdoptOpenJDK"

function get_prop
{
    grep "^${1}" "${PROPERTIES_FILE}" |cut -d'=' -f2
}

function get_os_name
{
    case "$(uname -s)" in
        Linux*)     local os=linux;;
        Darwin*)    local os=mac;;
        *)          echo "Unknown OS"; exit 1;;
    esac
    echo "$os"
}

function get_arch_name
{
    case "$(uname -m)" in
        x86_64)     local arch=x64;;
        *)          echo "Unknown architecture"; exit 1;;
    esac
    echo "$arch"
}

function download_maven
{
    echo "Downloading Maven ${MAVEN_VERSION}..."
    local maven_filename="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    local maven_file_path="${MAVEN_MIRROR}/${MAVEN_VERSION}/${maven_filename}"
    mkdir -p "${LOCAL_REPO}/maven"
    wget -q --show-progress -P "${LOCAL_REPO}/maven" "${maven_file_path}"
    echo "Extracting Maven..."
    tar xf "${LOCAL_REPO}/maven/${maven_filename}" -C "${LOCAL_REPO}/maven"
    rm "${LOCAL_REPO}/maven/${maven_filename}"
}

function get_java_major_version
{
    local array
    IFS='.+u-' read -ra array <<< "$JDK_VERSION"
    local major=${array[0]}
    echo "${major}"
}

function get_adoptopenjdk_url
{
    local major
    major=$(get_java_major_version)
    local jdk_dist
    if [ "${major}" -le "8" ]; then
        local file_version=${JDK_VERSION/-/} # Remove '-' (e.g. 8u192-b12 -> 8u192b12)
        jdk_dist="${JDK_MIRROR}/openjdk${major}-binaries/releases/download/jdk${JDK_VERSION}/OpenJDK${major}U-jdk_${ARCH_NAME}_${OS_NAME}_hotspot_${file_version}.tar.gz"
    else
        local file_version=${JDK_VERSION/+/_} # Replace '+' by '_' (e.g. 11.0.1_13 -> 11.0.1+13)
        jdk_dist="${JDK_MIRROR}/openjdk${major}-binaries/releases/download/jdk-${JDK_VERSION}/OpenJDK${major}U-jdk_${ARCH_NAME}_${OS_NAME}_hotspot_${file_version}.tar.gz"
    fi
    echo "${jdk_dist}"
}

function download_jdk
{
    echo "Downloading JDK ${JDK_VERSION}..."
    local jdk_dist
    case "${JDK_VENDOR}" in
        adoptopenjdk)    jdk_dist=$(get_adoptopenjdk_url);;
        *)               echo "Unknown JDK vendor ${JDK_VENDOR}"; exit 1;;
    esac
    echo "Downloading ${jdk_dist}"
    mkdir -p "${LOCAL_REPO}/jdk/${JDK_VENDOR}"
    wget -q --show-progress -P "${LOCAL_REPO}/jdk/${JDK_VENDOR}" "${jdk_dist}"
    local jdk_filename
    jdk_filename=$(basename "${jdk_dist}")
    echo "Extracting JDK..."
    tar xf "${LOCAL_REPO}/jdk/${JDK_VENDOR}/${jdk_filename}" -C "${LOCAL_REPO}/jdk/${JDK_VENDOR}"
    rm "${LOCAL_REPO}/jdk/${JDK_VENDOR}/${jdk_filename}"
}

MAVEN_VERSION=$(get_prop "maven.version")
MAVEN_MIRROR=$(get_prop "maven.mirror")
MAVEN_MIRROR=${MAVEN_MIRROR:-${DEFAULT_MAVEN_MIRROR}}

JDK_VERSION=$(get_prop "jdk.version")
JDK_VENDOR=$(get_prop "jdk.vendor")
JDK_VENDOR=${JDK_VENDOR:-"adoptopenjdk"}
JDK_VENDOR=$(echo "${JDK_VENDOR}" | tr '[:upper:]' '[:lower:]') # Convert to lower case
JDK_MIRROR=$(get_prop "jdk.mirror")
JDK_MIRROR=${JDK_MIRROR:-${DEFAULT_JDK_MIRROR}}

OS_NAME=$(get_os_name)
ARCH_NAME=$(get_arch_name)

if [ -z "${MAVEN_VERSION}" ]; then
    echo "Missing maven.version"
    exit 1
fi
if [ -z "${JDK_VERSION}" ]; then
    echo "Missing jdk.version"
    exit 1
fi

if [ ! -d "${LOCAL_REPO}/maven/apache-maven-${MAVEN_VERSION}" ]; then
    download_maven
fi
if [ ! -d "${LOCAL_REPO}/jdk/${JDK_VENDOR}/jdk${JDK_VERSION}" ]; then
    download_jdk
fi

JAVA_HOME="${LOCAL_REPO}/jdk/${JDK_VENDOR}/jdk${JDK_VERSION}" "${LOCAL_REPO}/maven/apache-maven-${MAVEN_VERSION}/bin/mvn" "$@"

