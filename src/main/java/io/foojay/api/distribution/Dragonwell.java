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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.ArchiveType.SRC_TAR;
import static eu.hansolo.jdktools.ArchiveType.getFromFileName;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;


public class Dragonwell implements Distribution {
    private static final Logger                       LOGGER                 = LoggerFactory.getLogger(Dragonwell.class);

    //private static final String                       GITHUB_USER_OLD            = "alibaba";
    //private static final String                       PACKAGE_URL_OLD            = "https://api.github.com/repos/" + GITHUB_USER + "/dragonwell";
    private static final String                       GITHUB_USER            = "dragonwell-project";
    private static final String                       PACKAGE_URL            = "https://api.github.com/repos/" + GITHUB_USER + "/dragonwell";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "";
    private static final String                       OPERATING_SYSTEM_PARAM = "";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "";
    private static final String                       RELEASE_STATUS_PARAM   = "";
    private static final String                       SUPPORT_TERM_PARAM     = "";
    private static final String                       BITNESS_PARAM          = "";

    private static final HashAlgorithm                HASH_ALGORITHM         = HashAlgorithm.NONE;
    private static final String                       HASH_URI               = "";
    private static final SignatureType                SIGNATURE_TYPE         = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM    = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI          = "";
    private static final String                       OFFICIAL_URI           = "http://dragonwell-jdk.io/";


    @Override public Distro getDistro() { return Distro.DRAGONWELL; }

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
        return List.of("dragonwell", "DRAGONWELL", "Dragonwell");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.DRAGONWELL.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);

        switch(versionNumber.getFeature().getAsInt()) {
            case 8,11,17 -> queryBuilder.append(versionNumber.getFeature().getAsInt()).append("/releases").append("?per_page=100");
            default -> { return ""; }
        }
        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);
        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;

        if (jsonObj.has("message")) {
            LOGGER.debug("Github rate limit reached when trying to get packages for Dragonwell {}", versionNumber);
            return pkgs;
        }

        String name = jsonObj.get("name").getAsString().strip();
        ReleaseStatus rs = Constants.RELEASE_STATUS_LOOKUP.entrySet().stream()
                                                          .filter(entry -> name.endsWith(entry.getKey()))
                                                          .findFirst()
                                                          .map(Entry::getValue)
                                                          .orElse(ReleaseStatus.GA);

        VersionNumber vNumber = null;
        String tag = jsonObj.get("tag_name").getAsString();
        if (tag.contains("_jdk")) {
            tag = tag.substring(tag.lastIndexOf("_jdk")).replace("_jdk", "");
            vNumber = VersionNumber.fromText(tag);
        }

            if (jsonObj.has("prerelease")) {
            if (jsonObj.get("prerelease").getAsBoolean()) { return pkgs; }
        }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     filename     = assetJsonObj.get("name").getAsString();
            if (filename.endsWith(Constants.FILE_ENDING_TXT) || filename.endsWith(Constants.FILE_ENDING_JAR) || filename.endsWith(Constants.FILE_ENDING_SOURCE_TAR_GZ)) { continue; }

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            Pkg pkg = new Pkg();

            ArchiveType ext = getFromFileName(filename);
            if (SRC_TAR == ext || (ArchiveType.NONE != archiveType && ext != archiveType)) { continue; }
            pkg.setArchiveType(ext);

            pkg.setDistribution(Distro.DRAGONWELL.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> filename.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);
            if (Architecture.NONE != architecture && architecture != arch) { continue; }
            if (Bitness.NONE != bitness && bitness != arch.getBitness()) { continue; }
            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

            if (null == vNumber) {
                vNumber = VersionNumber.fromText(downloadLink);
            }
            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { continue; }
            } else {
                //if (versionNumber.compareTo(vNumber) != 0) { continue; }
            }
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            VersionNumber dNumber = VersionNumber.fromText(filename);
            pkg.setDistributionVersion(dNumber);
            pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

            pkg.setTermOfSupport(supTerm);

            pkg.setPackageType(JDK);

            pkg.setReleaseStatus(rs);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> filename.contains(entry.getKey()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                switch (pkg.getArchiveType()) {
                    case DEB:
                    case RPM:
                    case TAR_GZ:
                        os = LINUX;
                        break;
                    case MSI:
                    case ZIP:
                        os = WINDOWS;
                        break;
                    case DMG:
                    case PKG:
                        os = MACOS;
                        break;
                    default: continue;
                }
            }
            if (OperatingSystem.NONE != operatingSystem && operatingSystem != os) { continue; }
            pkg.setOperatingSystem(os);

            pkg.setFreeUseInProduction(Boolean.TRUE);

            pkg.setSize(Helper.getFileSize(downloadLink));

            pkgs.add(pkg);
        }

        // Fetch checksums
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     filename     = assetJsonObj.get("name").getAsString();

            if (null == filename || filename.isEmpty() || (!filename.endsWith(Constants.FILE_ENDING_SHA256_TXT) && !filename.endsWith(Constants.FILE_ENDING_SHA256_DMG_TXT))) { continue; }
            String nameToMatch;
            if (filename.endsWith(Constants.FILE_ENDING_SHA256_DMG_TXT)) {
                nameToMatch = filename.replaceAll(Constants.FILE_ENDING_SHA256_DMG_TXT, "");
            } else if (filename.endsWith(Constants.FILE_ENDING_SHA256_TXT)) {
                nameToMatch = filename.replaceAll(Constants.FILE_ENDING_SHA256_TXT, "");
            } else {
                continue;
            }

            final String  downloadLink = assetJsonObj.get("browser_download_url").getAsString();
            Optional<Pkg> optPkg       = pkgs.stream().filter(pkg -> pkg.getFilename().contains(nameToMatch)).findFirst();
            if (optPkg.isPresent()) {
                Pkg pkg = optPkg.get();
                pkg.setChecksumUri(downloadLink);
                pkg.setChecksumType(HashAlgorithm.SHA256);
            }
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }
}
