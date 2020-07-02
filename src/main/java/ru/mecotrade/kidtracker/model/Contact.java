package ru.mecotrade.kidtracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contact {

    private ContactType type;

    private Integer index;

    private String phone;

    private String name;
}
