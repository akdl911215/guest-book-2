package com.jh.guestbook2.service;

import com.jh.guestbook2.dto.GuestbookDTO;
import com.jh.guestbook2.dto.PageRequestDTO;
import com.jh.guestbook2.dto.PageResultDTO;
import com.jh.guestbook2.entity.Guestbook;
import com.jh.guestbook2.entity.QGuestbook;
import com.jh.guestbook2.repository.GuestbookRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

@Service
@Log4j2
@RequiredArgsConstructor // 의존성 자동 주입
public class GuestbookServiceImpl implements GuestbookService{

    private final GuestbookRepository repository;

    @Override
    public GuestbookDTO read(Long gno) {

        Optional<Guestbook> result = repository.findById(gno);

        return result.isPresent() ? entityToDto(result.get()) : null;

    }

    @Override
    public GuestbookDTO register(GuestbookDTO dto) {

        Guestbook entity = dtoToEntity(dto);

        repository.save(entity);

        GuestbookDTO convertDto = entityToDto(entity);

        return convertDto;
    }

    @Override
    public GuestbookDTO remove(GuestbookDTO dto) {

        Optional<Guestbook> result = repository.findById(dto.getGno());

        if (result.isPresent()) {
            Guestbook entity = result.get();

            entity.changeDelDate(LocalDateTime.now());
            log.info("entity : " + entity);

            Guestbook convertEntity = repository.save(entity);
            log.info("convertEntity : " + convertEntity);

            GuestbookDTO response = entityToDto(convertEntity);
            log.info("response : " + response);

            return response;
        }

        return null;
    }

    @Override
    public GuestbookDTO modify(GuestbookDTO dto) {

        // 업데이트 하는 항목은 '제목', '내용'
        Optional<Guestbook> result = repository.findById(dto.getGno());

        if (result.isPresent()) {
            Guestbook entity = result.get();

            entity.changeTitle(dto.getTitle());
            entity.changeContent(dto.getContent());

            Guestbook convertEntity = repository.save(entity);

            GuestbookDTO response = entityToDto(convertEntity);

            return response;
        }

        return null;
    }

    @Override
    public PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO requestDTO) {

        Pageable pageable = requestDTO.getPageable(Sort.by("gno").descending());

        BooleanBuilder booleanBuilder = getSearch(requestDTO); // 검색 조건 처리

        Page<Guestbook> result = repository.findAll(booleanBuilder, pageable); // Querydsl 사용

        Function<Guestbook, GuestbookDTO> fn = (entity -> entityToDto(entity));

        return new PageResultDTO<>(result, fn);
    }

    private BooleanBuilder getSearch(PageRequestDTO requestDTO) {
        // Querydsl 처리
        String type = requestDTO.getType();

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        QGuestbook qGuestbook = QGuestbook.guestbook;

        String keyword = requestDTO.getKeyword();

        BooleanExpression expression = qGuestbook.gno.gt(0L);
        // gno > 0 조건만 생성

        booleanBuilder.and(expression);

        if (type == null || type.trim().length() == 0) {
            // 검색 조건이 없는 경우
            return booleanBuilder;
        }

        // 검색 조건을 작성하기
        BooleanBuilder conditionBuilder = new BooleanBuilder();

        if (type.contains("t")) { // 제목
            conditionBuilder.or(qGuestbook.title.contains(keyword));
        }
        if (type.contains("c")) { // 내용
            conditionBuilder.or(qGuestbook.content.contains(keyword));
        }
        if (type.contains("w")) { // 작성자
            conditionBuilder.or(qGuestbook.writer.contains(keyword));
        }

        // 모든 조건 통합
        booleanBuilder.and(conditionBuilder);

        return booleanBuilder;
    }
}

