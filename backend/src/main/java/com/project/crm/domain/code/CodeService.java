package com.project.crm.domain.code;
// 코드 비즈니스 로직

import com.project.crm.domain.code.dto.CodeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeRepository codeRepository;

    public Map<String, List<CodeResponseDto>> getGroupedCodes() {

        List<Code> codes = codeRepository.findAll();

        return codes.stream()
                .filter(code -> "Y".equals(code.getUseYn()))
                .collect(Collectors.groupingBy(
                        Code::getCodeGroup,
                        Collectors.mapping(
                                code -> new CodeResponseDto(
                                        code.getCodeVal(),
                                        code.getCodeNm()
                                ),
                                Collectors.toList()
                        )
                ));
    }

}