package com.ht.eventbox.modules.organization;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.organization.dtos.*;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.modules.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final CloudinaryService cloudinaryService;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    public List<Organization> getAll() {
        return organizationRepository.findAll();
    }

    public List<Organization> getByUserIdAndOrganizationRole(Long userId, OrganizationRole organizationRole) {
        return organizationRepository.findByUserOrganizationsUserIdAndUserOrganizationsRoleIs(userId, organizationRole);
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );
    }

    @Transactional
    public boolean create(Long userId, CreateOrganizationDto createOrganizationDto) {
        var org = Organization.builder()
                .name(createOrganizationDto.getName())
                .description(createOrganizationDto.getDescription())
                .phone(createOrganizationDto.getPhone())
                .paypalAccount(createOrganizationDto.getPaypalAccount())
                .email(createOrganizationDto.getEmail())
                .website(createOrganizationDto.getWebsite())
                .assets(new HashSet<>())
                .userOrganizations(new ArrayList<>())
                .build();

        var savedOrg = organizationRepository.save(org);

        savedOrg.getUserOrganizations().add(
                UserOrganization.builder()
                        .user(User.builder().id(userId).build())
                        .organization(Organization.builder().id(savedOrg.getId()).build())
                        .id(
                                UserOrganizationId.builder()
                                        .userId(userId)
                                        .organizationId(savedOrg.getId())
                                        .build()
                        )
                        .role(OrganizationRole.OWNER)
                        .build()
        );

        if (createOrganizationDto.getLogoBase64() != null && !createOrganizationDto.getLogoBase64().isEmpty()) {
            Map uploadResult = null;
            try {
                uploadResult = cloudinaryService.uploadByBase64(
                        createOrganizationDto.getLogoBase64(),
                        Constant.StorageFolder.ORGANIZATION_ASSETS
                );
                logger.info("Uploaded image: {}", uploadResult);
            } catch (IOException e) {
                throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (uploadResult == null)
                throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

            var asset = Asset.builder()
                    .resourceType(String.valueOf(uploadResult.get("resource_type")))
                    .publicId(String.valueOf(uploadResult.get("public_id")))
                    .signature(String.valueOf(uploadResult.get("signature")))
                    .id(String.valueOf(uploadResult.get("asset_id")))
                    .originalUrl(String.valueOf(uploadResult.get("url")))
                    .secureUrl(String.valueOf(uploadResult.get("secure_url")))
                    .folder(String.valueOf(uploadResult.get("folder")))
                    .format(String.valueOf(uploadResult.get("format")))
                    .width(Integer.parseInt(String.valueOf(uploadResult.get("width"))))
                    .height(Integer.parseInt(String.valueOf(uploadResult.get("height"))))
                    .bytes(Long.parseLong(String.valueOf(uploadResult.get("bytes"))))
                    .eTag(String.valueOf(uploadResult.get("etag")))
                    .usage(AssetUsage.AVATAR)
                    .build();

            savedOrg.getAssets().add(asset);
        }

        organizationRepository.save(savedOrg);
        return true;
    }

    @Transactional
    public boolean update(Long userId, Long orgId, UpdateOrganizationDto updateOrganizationDto) {
        var org = organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(orgId, userId, OrganizationRole.OWNER).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );

        org.setName(updateOrganizationDto.getName());
        org.setDescription(updateOrganizationDto.getDescription());
        org.setPhone(updateOrganizationDto.getPhone());
        org.setPaypalAccount(updateOrganizationDto.getPaypalAccount());
        org.setEmail(updateOrganizationDto.getEmail());
        org.setWebsite(updateOrganizationDto.getWebsite());

        Set<Asset> assetsToRemove = null;
        if (updateOrganizationDto.isRemoveLogo()) {
            org.getAssets().forEach(asset -> {
                try {
                    cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                    logger.info("Deleted image: {}", asset.getPublicId());
                } catch (IOException e) {
                    throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
            assetsToRemove = new HashSet<>(org.getAssets());
            org.getAssets().clear();
        } else if (updateOrganizationDto.getLogoBase64() != null && !updateOrganizationDto.getLogoBase64().isEmpty()) {
            org.getAssets().forEach(asset -> {
                try {
                    cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                    logger.info("Deleted image: {}", asset.getPublicId());
                } catch (IOException e) {
                    throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
            assetsToRemove = new HashSet<>(org.getAssets());
            org.getAssets().clear();
            Map uploadResult;
            try {
                uploadResult = cloudinaryService.uploadByBase64(
                        updateOrganizationDto.getLogoBase64(),
                        Constant.StorageFolder.ORGANIZATION_ASSETS
                );
                logger.info("Uploaded image: {}", uploadResult);
            } catch (IOException e) {
                throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (uploadResult == null)
                throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

            var asset = Asset.builder()
                    .resourceType(String.valueOf(uploadResult.get("resource_type")))
                    .publicId(String.valueOf(uploadResult.get("public_id")))
                    .signature(String.valueOf(uploadResult.get("signature")))
                    .id(String.valueOf(uploadResult.get("asset_id")))
                    .originalUrl(String.valueOf(uploadResult.get("url")))
                    .secureUrl(String.valueOf(uploadResult.get("secure_url")))
                    .folder(String.valueOf(uploadResult.get("folder")))
                    .format(String.valueOf(uploadResult.get("format")))
                    .width(Integer.parseInt(String.valueOf(uploadResult.get("width"))))
                    .height(Integer.parseInt(String.valueOf(uploadResult.get("height"))))
                    .bytes(Long.parseLong(String.valueOf(uploadResult.get("bytes"))))
                    .eTag(String.valueOf(uploadResult.get("etag")))
                    .usage(AssetUsage.AVATAR)
                    .build();

            org.getAssets().add(asset);
        }

        organizationRepository.save(org);
        if (assetsToRemove != null) {
            assetRepository.deleteAll(assetsToRemove);
        }
        return true;
    }

    @Transactional
    public boolean deleteById(Long userId, Long orgId) {
        var org = organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(orgId, userId, OrganizationRole.OWNER).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );

        org.getAssets().forEach(asset -> {
            try {
                cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                logger.info("Deleted image: {}", asset.getPublicId());
            } catch (IOException e) {
                throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

        organizationRepository.delete(org);
        assetRepository.deleteAll(org.getAssets());
        return true;
    }

    @Transactional
    public boolean addMember(Long userId, Long orgId, AddMemberDto addMemberDto) {
        var org = organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(orgId, userId, OrganizationRole.OWNER).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );

        var userOrg = org.getUserOrganizations().stream()
                .filter(uo -> uo.getUser().getEmail().equalsIgnoreCase(addMemberDto.getEmail()))
                .findFirst();

        if (userOrg.isPresent()) {
            throw new HttpException(Constant.ErrorCode.USER_ALREADY_IN_ORGANIZATION, HttpStatus.BAD_REQUEST);
        }

        var user = userRepository.findByEmail(addMemberDto.getEmail())
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        var newUserOrg = UserOrganization.builder()
                .user(user)
                .organization(org)
                .id(UserOrganizationId.builder()
                        .userId(user.getId())
                        .organizationId(org.getId())
                        .build())
                .role(addMemberDto.getRole().toOrganizationRole())
                .build();

        org.getUserOrganizations().add(newUserOrg);
        organizationRepository.save(org);

        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendMemberAddedEmail(
                        user.getEmail(),
                        user.getFullName(),
                        org.getName()
                );
            } catch (MessagingException e) {
                logger.error("mailService.sendRegistrationEmail: {}", e.getMessage());
            }
        });

        return true;
    }

    @Transactional
    public boolean updateMember(Long userId, Long orgId, UpdateMemberDto updateMemberDto) {
        var org = organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(orgId, userId, OrganizationRole.OWNER).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );

        var userOrg = org.getUserOrganizations().stream()
                .filter(uo -> uo.getUser().getEmail().equalsIgnoreCase(updateMemberDto.getEmail()) && !uo.getUser().getId().equals(userId))
                .findFirst();

        if (userOrg.isEmpty()) {
            throw new HttpException(Constant.ErrorCode.USER_NOT_IN_ORGANIZATION, HttpStatus.BAD_REQUEST);
        }

        userOrg.get().setRole(updateMemberDto.getRole().toOrganizationRole());
        organizationRepository.save(org);
        return true;
    }

    @Transactional
    public boolean removeMember(Long userId, Long orgId, RemoveMemberDto removeMemberDto) {
        var org = organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(orgId, userId, OrganizationRole.OWNER).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );

        var userOrg = org.getUserOrganizations().stream()
                .filter(uo -> uo.getUser().getEmail().equalsIgnoreCase(removeMemberDto.getEmail()) && !uo.getUser().getId().equals(userId))
                .findFirst();

        if (userOrg.isEmpty()) {
            throw new HttpException(Constant.ErrorCode.USER_NOT_IN_ORGANIZATION, HttpStatus.BAD_REQUEST);
        }

        org.getUserOrganizations().remove(userOrg.get());
        organizationRepository.save(org);

        CompletableFuture.runAsync(() -> {
            try {
                mailService.sendMemberRemovedEmail(
                        userOrg.get().getUser().getEmail(),
                        userOrg.get().getUser().getFullName(),
                        org.getName()
                );
            } catch (MessagingException e) {
                logger.error("mailService.sendMemberRemovedEmail: {}", e.getMessage());
            }
        });

        return true;
    }
}
