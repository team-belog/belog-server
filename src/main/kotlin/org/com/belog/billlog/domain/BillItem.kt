package org.com.belog.billlog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import java.math.BigDecimal

private const val BILL_ITEM_NAME_MIN_LENGTH = 1
private const val BILL_ITEM_NAME_MAX_LENGTH = 100

@Entity
@Table(
    name = "bill_log_bill_items",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bill_log_bill_items_bill_order",
            columnNames = ["bill_id", "item_order"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_bill_log_bill_items_name",
            constraint =
                "CHAR_LENGTH(TRIM(name)) BETWEEN $BILL_ITEM_NAME_MIN_LENGTH AND $BILL_ITEM_NAME_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_bill_log_bill_items_amount",
            constraint = "amount > 0",
        ),
        CheckConstraint(
            name = "chk_bill_log_bill_items_order",
            constraint = "item_order >= 0",
        ),
    ],
)
class BillItem protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "bill_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_bill_items_bill_id"),
    )
    val bill: Bill,
    @Column(nullable = false, length = BILL_ITEM_NAME_MAX_LENGTH)
    val name: String,
    @Column(nullable = false, precision = BillAmount.PRECISION, scale = BillAmount.SCALE)
    val amount: BigDecimal,
    @Column(name = "item_order", nullable = false)
    val itemOrder: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        const val NAME_MIN_LENGTH = BILL_ITEM_NAME_MIN_LENGTH
        const val NAME_MAX_LENGTH = BILL_ITEM_NAME_MAX_LENGTH

        fun create(
            bill: Bill,
            name: String,
            amount: BigDecimal,
            itemOrder: Int,
        ): BillItem {
            val normalizedName = name.trim()
            require(normalizedName.length in NAME_MIN_LENGTH..NAME_MAX_LENGTH) {
                "결제 항목명은 ${NAME_MIN_LENGTH}자 이상 ${NAME_MAX_LENGTH}자 이하여야 합니다."
            }
            require(amount.signum() > 0) {
                "결제 항목 금액은 0보다 커야 합니다."
            }
            require(bill.currency.supports(amount) && BillAmount.isStorable(amount)) {
                "결제 항목 금액이 화폐 단위의 허용 범위를 벗어났습니다."
            }
            require(itemOrder >= 0) {
                "결제 항목 순서는 0 이상이어야 합니다."
            }

            return BillItem(
                bill = bill,
                name = normalizedName,
                amount = bill.currency.normalize(amount),
                itemOrder = itemOrder,
            )
        }
    }
}
