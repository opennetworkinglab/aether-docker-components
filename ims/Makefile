# SPDX-FileCopyrightText: 2021 Open Networking Foundation <info@opennetworking.org>
# Copyright 2019 free5GC.org
#
# SPDX-License-Identifier: Apache-2.0
#
#

PROJECT_NAME             := ims
DOCKER_VERSION           ?= $(shell cat ./VERSION)

## Docker related
DOCKER_REGISTRY          ?=
DOCKER_REPOSITORY        ?=
DOCKER_TAG               ?= ${DOCKER_VERSION}
DOCKER_BUILDKIT          ?= 1
DOCKER_BUILD_ARGS        ?=

## Docker labels with error handling
DOCKER_LABEL_VCS_URL     ?= $(shell git remote get-url origin 2>/dev/null || echo "unknown")
DOCKER_LABEL_VCS_REF     ?= $(shell \
        echo "$${GIT_COMMIT:-$${GITHUB_SHA:-$${CI_COMMIT_SHA:-$(shell \
                if git rev-parse --git-dir > /dev/null 2>&1; then \
                        git rev-parse HEAD 2>/dev/null; \
                else \
                        echo "unknown"; \
                fi \
        )}}}")
DOCKER_LABEL_BUILD_DATE  ?= $(shell date -u "+%Y-%m-%dT%H:%M:%SZ")
CWD                      := $(shell pwd)
KAMAILIO-TARGET		 := "ims-kamailio"

## Upstream source refs (must match Dockerfile ARG defaults)

DOCKER_TARGETS           ?= dns mysql rtpengine n5 pyhss ims_base

.PHONY: docker-build docker-push

.DEFAULT_GOAL: docker-build

docker-build:
	for target in $(DOCKER_TARGETS); do \
                case $$target in \
                        ims_base) _IMAGE_NAME="${DOCKER_REGISTRY}${DOCKER_REPOSITORY}${KAMAILIO-TARGET}:${DOCKER_TAG}" ;; \
                        *)      _IMAGE_NAME="${DOCKER_REGISTRY}${DOCKER_REPOSITORY}${PROJECT_NAME}-$$target:${DOCKER_TAG}" ;; \
                esac; \
                cd $(CWD)/$$target && \
                DOCKER_BUILDKIT=$(DOCKER_BUILDKIT) docker build $(DOCKER_BUILD_ARGS) \
                        --file Dockerfile \
                        --tag $$_IMAGE_NAME \
                        --build-arg VERSION="${DOCKER_VERSION}" \
                        --build-arg VCS_URL="${DOCKER_LABEL_VCS_URL}" \
                        --build-arg VCS_REF="${DOCKER_LABEL_VCS_REF}" \
                        --build-arg BUILD_DATE="${DOCKER_LABEL_BUILD_DATE}" \
                        $$_TARGET_BUILD_ARGS \
                        . \
                        || exit 1; \
        done

docker-push:
	for target in $(DOCKER_TARGETS); do \
                case $$target in \
                        ims_base) _IMAGE_NAME="${DOCKER_REGISTRY}${DOCKER_REPOSITORY}${KAMAILIO-TARGET}:${DOCKER_TAG}" ;; \
                        *)      _IMAGE_NAME="${DOCKER_REGISTRY}${DOCKER_REPOSITORY}${PROJECT_NAME}-$$target:${DOCKER_TAG}" ;; \
                esac; \
                docker push $$_IMAGE_NAME; \
        done
