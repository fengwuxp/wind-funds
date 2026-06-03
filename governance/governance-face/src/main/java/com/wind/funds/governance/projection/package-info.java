/**
 * 交易投影重放契约包。
 *
 * <p>本包只放置交易投影重放对外可见的请求、范围、checkpoint、事实、重建行、差异和端口契约。
 * 它负责定义交易投影如何被只读核对、影子重建和正式重建，不承载具体 DAL、事务编排、账务处理或余额处理。</p>
 */
package com.wind.funds.governance.projection;
