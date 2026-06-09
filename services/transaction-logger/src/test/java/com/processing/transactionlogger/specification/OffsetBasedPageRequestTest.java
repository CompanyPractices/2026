package com.processing.transactionlogger.specification;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

public class OffsetBasedPageRequestTest {
    @Test
    void getOffsetReturnsConfiguredOffset() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(25, 50);
        assertThat(pageRequest.getOffset()).isEqualTo(25);
    }

    @Test
    void getPageSizeReturnsConfiguredLimit() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(25, 50);
        assertThat(pageRequest.getPageSize()).isEqualTo(50);
    }

    @Test
    void getSortReturnsByCreatedAtDesc() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);
        Sort sort = pageRequest.getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void nextAdvancesOffestByLimit() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);
        Pageable next = pageRequest.next();
        assertThat(next.getOffset()).isEqualTo(50);
    }

    @Test
    void firstReturnsOffsetZero() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(100, 50);
        Pageable first = pageRequest.first();
        assertThat(first.getOffset()).isEqualTo(0);
        assertThat(first.getPageSize()).isEqualTo(50);
    }

    @Test
    void hasPreviousReturnsFalseWhenOffsetIsZero() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);
        assertThat(pageRequest.hasPrevious()).isFalse();
    }

    @Test
    void hasPreviousReturnsTrueWhenOffsetIsPositive() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(10, 50);
        assertThat(pageRequest.hasPrevious()).isTrue();
    }

    @Test
    void previousOrFirstReturnsPreviousPageWhenHasPrevious() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(100, 50);
        Pageable previous = pageRequest.previousOrFirst();
        assertThat(previous.getOffset()).isEqualTo(50);
    }

    @Test
    void previousOrFirstReturnsFirstWhenAtStart() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);
        Pageable previous = pageRequest.previousOrFirst();
        assertThat(previous.getOffset()).isEqualTo(0);
    }

    @Test
    void withPageReturnsCorrectOffset() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);
        Pageable secondPage = pageRequest.withPage(2);
        assertThat(secondPage.getOffset()).isEqualTo(100);
    }
}
