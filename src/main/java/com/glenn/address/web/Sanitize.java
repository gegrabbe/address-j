package com.glenn.address.web;

import com.glenn.address.domain.Address;
import com.glenn.address.domain.Entry;
import com.glenn.address.domain.Person;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.Charset;
import java.util.List;

public class Sanitize {

    public static String fix(String value) {
        return HtmlUtils.htmlEscape(value, Charset.defaultCharset().name());
    }

    public static Entry one(Entry value) {
        return new Entry(
                value.entryId(),
                new Person(
                        fix(value.person().firstName()),
                        fix(value.person().lastName()),
                        value.person().age(),
                        value.person().gender(),
                        value.person().maritalStatus()
                ),
                new Address(
                        fix(value.address().street()),
                        fix(value.address().city()),
                        fix(value.address().state()),
                        fix(value.address().zip()),
                        fix(value.address().email()),
                        fix(value.address().phone())
                ),
                fix(value.notes())
        );
    }

    public static List<Entry> many(List<Entry> value) {
        return value.stream()
                .map(Sanitize::one)
                .toList();
    }

}
