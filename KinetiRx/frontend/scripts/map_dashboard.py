import sys; sys.path.insert(0,'.')
from i18n_tool import run, text, lit
F='../src/components/tabs/DashboardTab.tsx'
J=[
 ('Pharmacy Overview & Live Analytics','نظرة عامة على الصيدلية والتحليلات المباشرة','heroTitle'),
 ("Real-time counter sales, cash drawer reconciliation, patient dues, and pharmaceutical inventory for today.","مبيعات الكاشير والصندوق النقدي ومستحقات المرضى ومخزون الأدوية لهذا اليوم، في الوقت الفعلي.",'heroSubtitle'),
 ('Ask AI Pharmacist','اسأل الصيدلي الذكي','askAi'),
 ('+ POS Billing Counter','+ شباك فوترة نقطة البيع','posCounter'),
 ('6-Month Short Expiry Alert','تنبيه: أدوية تنتهي خلال 6 أشهر','expiryAlert'),
 ('View expiring batches & return list','عرض الدفعات القريبة الانتهاء وقائمة الإرجاع','expiryAlertHint'),
 ('Upcoming OPD Follow-ups','متابعات العيادة الخارجية القادمة','followUps'),
 ('Send WhatsApp follow-up reminders','إرسال تذكيرات المتابعة عبر واتساب','followUpsHint'),
 ("Today's Sales",'مبيعات اليوم','todaySales'),
 ('Total Due','إجمالي المستحقات','totalDue'),
 ('Drawer Cash','النقد في الصندوق','drawerCash'),
 ('Physical Closing Cash','النقد الفعلي عند الإغلاق','physicalCash'),
 ('Stock SKUs','عدد الأصناف في المخزون','stockSkus'),
 ('Stock Valuation','تقييم المخزون','stockValuation'),
 ('Purchase valuation','قيمة الشراء','purchaseValuation'),
 ('Revenue vs Expenses','الإيرادات مقابل المصروفات','revenueVsExpenses'),
 ('7-Day Financial Flow','التدفق المالي لآخر 7 أيام','sevenDayFlow'),
 ('Live','مباشر','live'),
 ('Payment Breakdown','توزيع طرق الدفع','paymentBreakdown'),
 ('Cash vs UPI vs Due Balance','نقدي مقابل إلكتروني مقابل المستحقات','paymentBreakdownHint'),
 ('Current','الحالي','current'),
 ('Inventory Turnover','دوران المخزون','inventoryTurnover'),
 ('Units in Stock vs Sold','الوحدات في المخزون مقابل المباعة','inventoryTurnoverHint'),
]
L=[
 ('Cash in Drawer','النقد في الصندوق','cashInDrawer'),
 ('PhonePe / UPI','PhonePe / UPI','phonePeUpi'),
 ('Due Balance','رصيد المستحقات','dueBalance'),
 ('In-Hand Stock','المخزون المتوفر','inHandStock'),
 ('Sold Out Units','الوحدات المباعة','soldOutUnits'),
 ('Low Stock (<10)','مخزون منخفض (أقل من 10)','lowStock'),
 ('No critical expiring medicines','لا توجد أدوية حرجة قريبة الانتهاء','noCriticalExpiry'),
 ('No follow-up visits pending','لا توجد متابعات معلقة','noFollowUps'),
]
ops=[text(en,ar,'dashboard.'+k) for en,ar,k in J]+[lit(en,ar,'dashboard.'+k) for en,ar,k in L]
# keys are namespaced inside the op; run() prefixes the namespace
ops=[text(en,ar,k) for en,ar,k in J]+[lit(en,ar,k) for en,ar,k in L]
run(F,'dashboard',ops,hook_after="  const currencySymbol = getCurrencySymbol(invoiceConfig?.currency);")
