/*
 * Copyright (c) 2021.
 *
 * This file is part of DiscoAPI.
 *
 *     DiscoAPI is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     DiscoAPI is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.distribution;

import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.FPU;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.Verification;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Feature;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.Architecture.AARCH64;
import static eu.hansolo.jdktools.Architecture.AMD64;
import static eu.hansolo.jdktools.Architecture.ARM;
import static eu.hansolo.jdktools.Architecture.PPC;
import static eu.hansolo.jdktools.Architecture.SPARC;
import static eu.hansolo.jdktools.Architecture.X64;
import static eu.hansolo.jdktools.Architecture.X86;
import static eu.hansolo.jdktools.ArchiveType.DEB;
import static eu.hansolo.jdktools.ArchiveType.DMG;
import static eu.hansolo.jdktools.ArchiveType.MSI;
import static eu.hansolo.jdktools.ArchiveType.PKG;
import static eu.hansolo.jdktools.ArchiveType.RPM;
import static eu.hansolo.jdktools.ArchiveType.SRC_TAR;
import static eu.hansolo.jdktools.ArchiveType.TAR_GZ;
import static eu.hansolo.jdktools.ArchiveType.ZIP;
import static eu.hansolo.jdktools.Bitness.BIT_32;
import static eu.hansolo.jdktools.Bitness.BIT_64;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.LINUX_MUSL;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.SOLARIS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;
import static eu.hansolo.jdktools.TermOfSupport.LTS;
import static eu.hansolo.jdktools.TermOfSupport.MTS;
import static eu.hansolo.jdktools.TermOfSupport.STS;


public class Liberica implements Distribution {
    private static final Logger                       LOGGER                          = LoggerFactory.getLogger(Liberica.class);

    private static final String                       PACKAGE_URL                     = "https://api.bell-sw.com/v1/liberica/releases";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM              = "arch";
    private static final String                       OPERATING_SYSTEM_PARAM          = "os";
    private static final String                       ARCHIVE_TYPE_PARAM              = "package-type";
    private static final String                       PACKAGE_TYPE_PARAM              = "bundle-type";
    private static final String                       RELEASE_STATUS_PARAM            = "build-type";
    private static final String                       SUPPORT_TERM_PARAM              = "release-type";
    private static final String                       BITNESS_PARAM                   = "bitness";
    private static final String                       FX_PARAM                        = "fx";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP                = Map.of(ARM, "arm", PPC, "ppc", SPARC, "sparc", X86, "x86", X64, "x64", AMD64, "amd64", AARCH64, "aarch64");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP            = Map.of(LINUX, "linux", LINUX_MUSL, "linux_musl", MACOS, "macos", WINDOWS, "windows", SOLARIS, "solaris");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP                = Map.of(DEB, "deb", DMG, "dmg", MSI, "msi", PKG, "pkg", RPM, "rpm", SRC_TAR, "src.tar.gz", TAR_GZ, "tar.gz", ZIP, "zip");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP                = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP              = Map.of(EA, "ea", GA, "all");
    private static final Map<TermOfSupport, String>   TERMS_OF_SUPPORT_MAP            = Map.of(STS, "sts", MTS, "mts", LTS, "lts");
    private static final Map<Bitness, String>         BITNESS_MAP                     = Map.of(BIT_32, "32", BIT_64, "64");

    // JSON fields
    private static final String                       FIELD_FILENAME                  = "filename";
    private static final String                       FIELD_DOWNLOAD_URL              = "downloadUrl";
    private static final String                       FIELD_FEATURE_VERSION           = "featureVersion";
    private static final String                       FIELD_INTERIM_VERSION           = "interimVersion";
    private static final String                       FIELD_UPDATE_VERSION            = "updateVersion";
    private static final String                       FIELD_PATCH_VERSION             = "patchVersion";
    private static final String                       FIELD_BUILD_VERSION             = "buildVersion";
    private static final String                       FIELD_FX                        = "FX";
    private static final String                       FIELD_PACKAGE_TYPE              = "packageType";
    private static final String                       FIELD_BUNDLE_TYPE               = "bundleType";
    private static final String                       FIELD_GA                        = "GA";
    private static final String                       FIELD_LTS                       = "LTS";
    private static final String                       FIELD_BITNESS                   = "bitness";
    private static final String                       FIELD_OS                        = "os";
    private static final String                       FIELD_ARCHITECTURE              = "architecture";
    private static final String                       FIELD_SHA1                      = "sha1";

    private static final HashAlgorithm                HASH_ALGORITHM             = HashAlgorithm.NONE;
    private static final String                       HASH_URI                   = "";
    private static final SignatureType                SIGNATURE_TYPE             = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM        = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI              = "";
    private static final String                       OFFICIAL_URI               = "https://bell-sw.com/";


    @Override public Distro getDistro() { return Distro.LIBERICA; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return SUPPORT_TERM_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }

    @Override public HashAlgorithm getHashAlgorithm() { return HASH_ALGORITHM; }

    @Override public String getHashUri() { return HASH_URI; }

    @Override public SignatureType getSignatureType() { return SIGNATURE_TYPE; }

    @Override public HashAlgorithm getSignatureAlgorithm() { return SIGNATURE_ALGORITHM; }

    @Override public String getSignatureUri() { return SIGNATURE_URI; }

    @Override public String getOfficialUri() { return OFFICIAL_URI; }

    @Override public List<String> getSynonyms() {
        return List.of("liberica", "LIBERICA", "Liberica");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.LIBERICA.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);
        int initialSize = queryBuilder.length();

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("version-feature=").append(versionNumber.getFeature().getAsInt());

        /*
        if (versionNumber.getInterim().isPresent()) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append("version-interim=").append(versionNumber.getInterim().getAsInt());
        }

        if (versionNumber.getUpdate().isPresent()) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append("version-update=").append(versionNumber.getUpdate().getAsInt());
        }

        if (latest) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append("version-modifier=").append("latest");
        }
        */

        if (bitness != Bitness.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            if (Architecture.X64 == architecture) {
                queryBuilder.append(BITNESS_PARAM).append("=").append(architecture.getBitness().getAsString());
            } else {
                queryBuilder.append(BITNESS_PARAM).append("=").append(BITNESS_MAP.get(bitness));
            }
        }

        if (null != javafxBundled) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(FX_PARAM).append("=").append(javafxBundled);
        }

        if (releaseStatus != ReleaseStatus.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(RELEASE_STATUS_PARAM).append("=").append(RELEASE_STATUS_MAP.get(releaseStatus));
        }

        if (termOfSupport != TermOfSupport.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(SUPPORT_TERM_PARAM).append("=").append(TERMS_OF_SUPPORT_MAP.get(termOfSupport));
        }

        if (operatingSystem != OperatingSystem.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(OPERATING_SYSTEM_PARAM).append("=").append(OPERATING_SYSTEM_MAP.get(operatingSystem));
        }

        if (architecture != Architecture.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHITECTURE_PARAM).append("=").append(ARCHITECTURE_MAP.get(architecture));
            if (Architecture.X64 == architecture && !queryBuilder.toString().contains(BITNESS_PARAM)) {
                queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
                queryBuilder.append(BITNESS_PARAM).append("=").append(architecture.getBitness().getAsString());
            }
        }

        if (archiveType != ArchiveType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHIVE_TYPE_PARAM).append("=").append(ARCHIVE_TYPE_MAP.get(archiveType));
        }

        if (packageType != PackageType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(packageType));
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType bundleType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        String        filename      = jsonObj.get(FIELD_FILENAME).getAsString();
        String        downloadLink  = jsonObj.get(FIELD_DOWNLOAD_URL).getAsString();
        VersionNumber vNumber       = new VersionNumber(jsonObj.get(FIELD_FEATURE_VERSION).getAsInt(), jsonObj.get(FIELD_INTERIM_VERSION).getAsInt(), jsonObj.get(FIELD_UPDATE_VERSION).getAsInt(), jsonObj.get(FIELD_PATCH_VERSION).getAsInt());
        VersionNumber dNumber       = new VersionNumber(versionNumber);
        Integer       buildVersion  = jsonObj.get(FIELD_BUILD_VERSION).getAsInt();

        if (onlyNewPkgs) {
            if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { return pkgs; }
        }

        dNumber.setBuild(buildVersion);
        vNumber.setBuild(buildVersion);
        String        packageType   = jsonObj.get(FIELD_PACKAGE_TYPE).toString().replaceAll("\"", "");
        String        bundleTyp     = jsonObj.get(FIELD_BUNDLE_TYPE).toString().replaceAll("\"", "");
        boolean       isGA          = jsonObj.get(FIELD_GA).getAsBoolean();
        boolean       isFX          = jsonObj.get(FIELD_FX).getAsBoolean() || filename.contains("-full");
        boolean       isLTS         = jsonObj.get(FIELD_LTS).getAsBoolean();
        Integer       bits          = jsonObj.get(FIELD_BITNESS).getAsInt();
        String        os            = jsonObj.get(FIELD_OS).getAsString();
        String        arc           = jsonObj.get(FIELD_ARCHITECTURE).getAsString();

        if (latest) {
            if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
        }
        /*else { // Leads to problems since default interim, update and patch will be set to 0
            if (!versionNumber.equals(vNumber)) { return pkgs; }
        }
        */

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;

        if (null != javafxBundled) {
            if (javafxBundled != isFX) { return pkgs; }
        }

        if (OperatingSystem.NONE != operatingSystem && !OPERATING_SYSTEM_MAP.containsKey(operatingSystem)) { return pkgs; }

        Pkg pkg = new Pkg();
        pkg.setDistribution(Distro.LIBERICA.get());
        pkg.setVersionNumber(vNumber);
        pkg.setJavaVersion(vNumber);
        pkg.setDistributionVersion(dNumber);
        pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

        switch (bundleType) {
            case JDK:
                if (bundleTyp.toLowerCase().contains(Constants.JRE)) { return pkgs; }
                pkg.setPackageType(JDK);
                break;
            case JRE:
                if (bundleTyp.toLowerCase().contains(Constants.JDK)) { return pkgs; }
                pkg.setPackageType(JRE);
                break;
            case NONE:
            default:
                pkg.setPackageType(bundleTyp.toLowerCase().contains(Constants.JRE) ? JRE : JDK);
                break;
        }

        if (ArchiveType.NONE != archiveType && !packageType.equals(archiveType.getUiString())) { return pkgs; }
        ArchiveType ext = ArchiveType.fromText(packageType);
        if (ArchiveType.SRC_TAR == ext) { return pkgs; }
        pkg.setArchiveType(ArchiveType.fromText(packageType));

        Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                         .filter(entry -> filename.contains(entry.getKey()))
                                                         .findFirst()
                                                         .map(Entry::getValue)
                                                         .orElse(Architecture.NONE);

        if (filename.contains("hflt")) {
            pkg.setFPU(FPU.HARD_FLOAT);
        }

        Bitness bit = arch.getBitness();

        if (Architecture.NONE == arch) {
            LOGGER.debug("Architecture not found in Liberica for filename: {}", filename);
            return pkgs;
        }

        pkg.setArchitecture(arch);
        pkg.setBitness(bit);

        OperatingSystem osFound = OperatingSystem.fromText(os);
        if (OperatingSystem.NONE == osFound) {
            osFound = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                       .filter(entry -> filename.contains(entry.getKey()))
                                                       .findFirst()
                                                       .map(Entry::getValue)
                                                       .orElse(OperatingSystem.NONE);
        }
        if (OperatingSystem.NONE == osFound) {
            LOGGER.debug("Operating Sytsem not found in Liberica for filename: {}", filename);
            return pkgs;
        }
        pkg.setOperatingSystem(OperatingSystem.NONE == osFound ? OperatingSystem.fromText(os) : osFound);

        pkg.setJavaFXBundled(isFX);
        pkg.setReleaseStatus(isGA ? ReleaseStatus.GA : releaseStatus);

        pkg.setTckTested(isGA ? Verification.YES : Verification.NO);
        pkg.setTckCertUri(isGA ? "https://bell-sw.com/libericajdk/" : "");

        pkg.setTermOfSupport(supTerm);

        pkg.setFileName(filename);
        pkg.setDirectDownloadUri(downloadLink);

        pkg.setFreeUseInProduction(Boolean.TRUE);

        if (filename.contains("crac")) {
            pkg.getFeatures().add(Feature.CRAC);
        }

        if (jsonObj.has(FIELD_SHA1)) {
            String hash = jsonObj.get(FIELD_SHA1).getAsString();
            pkg.setChecksum(hash.isEmpty() ? "" : hash);
            pkg.setChecksumType(hash.isEmpty() ? HashAlgorithm.NONE : HashAlgorithm.SHA1);
        }

        pkg.setSize(Helper.getFileSize(downloadLink));

        pkgs.add(pkg);

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }
}
