package com.processing.transactionlogger.specification;

import com.processing.transactionlogger.model.Transaction_;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class OffsetBasedPageRequestTest {
    @Test
    void getOffsetReturnsConfiguredOffset() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(25, 50);

        assertEquals(25, pageRequest.getOffset());
    }

    @Test
    void getPageSizeReturnsConfiguredLimit() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(25, 50);

        assertEquals(50, pageRequest.getPageSize());
    }

    @Test
    void getSortReturnsDefaultCreatedAtDescWhenNotSpecified() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);

        Sort sort = pageRequest.getSort();

        assertNotNull(sort.getOrderFor(Transaction_.CREATED_AT));
        assertEquals(Sort.Direction.DESC, sort.getOrderFor(Transaction_.CREATED_AT).getDirection());
    }

    @Test
    void getSortReturnsCustomSortWhenProvided() {
        Sort customSort = Sort.by(Sort.Direction.ASC, "amount");

        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50, customSort);

        assertEquals(customSort, pageRequest.getSort());
    }

    @Test
    void nextAdvancesOffestByLimit() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);

        Pageable next = pageRequest.next();

        assertEquals(50, next.getOffset());
    }

    @Test
    void firstReturnsOffsetZero() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(100, 50);

        Pageable first = pageRequest.first();

        assertThat(first.getOffset()).isZero();
        assertEquals(50, first.getPageSize());
    }

    @Test
    void hasPreviousReturnsFalseWhenOffsetIsZero() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);

        assertFalse(pageRequest.hasPrevious());
    }

    @Test
    void hasPreviousReturnsTrueWhenOffsetIsPositive() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(10, 50);

        assertTrue(pageRequest.hasPrevious());
    }

    @Test
    void previousOrFirstReturnsPreviousPageWhenHasPrevious() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(100, 50);

        Pageable previous = pageRequest.previousOrFirst();

        assertEquals(50, previous.getOffset());
    }

    @Test
    void previousOrFirstReturnsFirstWhenAtStart() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);

        Pageable previous = pageRequest.previousOrFirst();

        assertThat(previous.getOffset()).isZero();
    }

    @Test
    void withPageReturnsCorrectOffset() {
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50);

        Pageable secondPage = pageRequest.withPage(2);

        assertEquals(100, secondPage.getOffset());
    }

    @Test
    void withPagePreservesSort() {
        Sort customSort = Sort.by(Sort.Direction.ASC, "amount");
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(0, 50, customSort);

        Pageable next = pageRequest.withPage(1);

        assertEquals(customSort, next.getSort());
    }
}
