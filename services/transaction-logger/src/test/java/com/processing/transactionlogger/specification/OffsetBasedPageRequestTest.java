package com.processing.transactionlogger.specification;

import com.processing.transactionlogger.model.Transaction_;
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
    void getSortReturnsDefaultCreatedAtDescWhenNotSpecified() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);

        Sort sort = pageRequest.getSort();

        assertThat(sort.getOrderFor(Transaction_.CREATED_AT)).isNotNull();
        assertThat(sort.getOrderFor(Transaction_.CREATED_AT).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getSortReturnsCustomSortWhenProvided() {
        Sort customSort = Sort.by(Sort.Direction.ASC, "amount");

        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50, customSort);

        assertThat(pageRequest.getSort()).isEqualTo(customSort);
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

    @Test
    void withPagePreservesSort() {
        Sort customSort = Sort.by(Sort.Direction.ASC, "amount");
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50, customSort);

        Pageable next = pageRequest.withPage(1);

        assertThat(next.getSort()).isEqualTo(customSort);
    }
}
