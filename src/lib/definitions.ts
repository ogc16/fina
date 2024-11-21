//defining the variables of Invoice
export type Invoice = {
    id: string;
    customer_name: string;
    customer_email: string;
    invoice_date: string;
    invoice_due_date: string;
    invoice_amount: number;
    invoice_status: "pending"|'paid';
    sent:'yes'|'no';
    customer_id:string;
};