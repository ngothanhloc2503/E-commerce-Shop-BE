package com.store.ecommerce.dto.response;

import com.store.ecommerce.entity.Address;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressBookDTO {
    List<Address> addressBook;
    boolean primaryAddressAsDefault;
}
